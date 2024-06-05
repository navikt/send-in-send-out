package no.nav.emottak.smtp.cpasync

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.SftpException
import io.ktor.client.HttpClient
import no.nav.emottak.deleteCPAinCPARepo
import no.nav.emottak.getCPATimestamps
import no.nav.emottak.nfs.NFSConnector
import no.nav.emottak.putCPAinCPARepo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class CpaSyncService(private val cpaRepoClient: HttpClient, private val nfsConnector: NFSConnector) {

    private val log: Logger = LoggerFactory.getLogger("no.nav.emottak.smtp.cpasync")

    suspend fun sync() {
        return runCatching {
            val cpaTimestamps = cpaRepoClient.getCPATimestamps()
            processAndSyncEntries(cpaTimestamps)
        }.onFailure {
            logFailure(it)
        }.getOrThrow()
    }

    private suspend fun processAndSyncEntries(cpaTimestamps: Map<String, String>) {
        nfsConnector.use { connector ->
            val staleCpaTimestamps = connector.folder().asSequence().filter { entry -> isValidFileType(entry) }
                .fold(cpaTimestamps) { accumulatedCpaTimestamps, entry ->
                    val filename = entry.filename
                    log.info("Checking $filename...")

                    val lastModified = Date(entry.attrs.mTime.toLong() * 1000).toInstant()
                    val shouldSkip = shouldSkipFile(filename, lastModified, accumulatedCpaTimestamps)
                    if (!shouldSkip) {
                        runCatching {
                            log.info("Fetching file $filename")
                            val cpaFileContent = connector.file("/outbound/cpa/$filename").use {
                                String(it.readAllBytes())
                            }
                            log.info("Uploading $filename")
                            cpaRepoClient.putCPAinCPARepo(cpaFileContent, lastModified)
                        }.onFailure {
                            log.error("Error uploading $filename to cpa-repo: ${it.message}", it)
                        }
                    }

                    filterStaleCpaTimestamps(filename, lastModified, accumulatedCpaTimestamps)
                }

            deleteStaleCpaEntries(staleCpaTimestamps)
        }
    }

    private fun isValidFileType(entry: ChannelSftp.LsEntry) = if (!entry.filename.endsWith(".xml")) {
        log.warn("${entry.filename} is ignored")
        false
    } else {
        true
    }

    private fun filterStaleCpaTimestamps(
        filename: String,
        lastModified: Instant,
        cpaTimestamps: Map<String, String>
    ): Map<String, String> {
        return cpaTimestamps.filter { (cpaId, timestamp) -> isStaleCpa(cpaId, filename, timestamp, lastModified) }
    }

    private fun isStaleCpa(
        cpaId: String,
        filename: String,
        timestamp: String,
        lastModified: Instant
    ): Boolean {
        val formattedCpaId = cpaId.replace(":", ".")
        return if (filename.contains(formattedCpaId)) {
            if (timestamp == lastModified.toString()) {
                log.info("$filename already exists with same timestamp")
            } else {
                log.info("$filename has different timestamp, should be updated")
            }
            false
        } else {
            true // delete!
        }
    }

    internal fun shouldSkipFile(
        filename: String,
        lastModified: Instant,
        cpaTimestamps: Map<String, String>
    ): Boolean {
        return cpaTimestamps.any { (cpaId, timestamp) ->
            val formattedCpaId = cpaId.replace(":", ".")
            if (filename.contains(formattedCpaId) && timestamp == lastModified.toString()) {
                log.info("Newer version already exists $filename, skipping...")
                true
            } else {
                false
            }
        }
    }

    internal suspend fun deleteStaleCpaEntries(cpaTimestamps: Map<String, String>) {
        cpaTimestamps.forEach { (cpaId) ->
            cpaRepoClient.deleteCPAinCPARepo(cpaId)
        }
    }

    internal fun logFailure(throwable: Throwable) {
        when (throwable) {
            is SftpException -> log.error("SftpException ID: [${throwable.id}]", throwable)
            else -> log.error(throwable.message, throwable)
        }
    }
}
