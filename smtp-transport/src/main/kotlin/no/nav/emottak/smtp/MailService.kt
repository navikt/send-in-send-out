package no.nav.emottak.smtp

import arrow.fx.coroutines.parMap
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import net.logstash.logback.marker.Markers
import no.nav.emottak.configuration.Config
import no.nav.emottak.configuration.Ebms
import no.nav.emottak.postEbmsMessageMultiPart
import no.nav.emottak.postEbmsMessageSinglePart
import no.nav.emottak.smtp.StoreFactory.createStore
import java.time.Duration
import java.time.Instant
import kotlin.Int.Companion.MAX_VALUE
import kotlin.time.toKotlinDuration

class MailService(private val config: Config) {
    private val httpClient = HttpClient(CIO)

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    suspend fun processMessages(): Flow<Result<HttpResponse>> {
        var messages = emptyFlow<Result<HttpResponse>>()
        val timeStart = Instant.now()
        runCatching {
            MailReader(config.mail, createStore(config.smtp), false).use { reader ->
                log.info("Starting to read ${reader.count()} messages from inbox")
                messages = reader.readMail()
                    .also { log.info("Finished reading all messages from inbox") }
                    .asFlow()
                    .also { log.info("Starting to process all messages read from inbox") }
                    .parMap(concurrency = MAX_VALUE) { postEbmsMessages(config.ebms, it) }
                    .flowOn(Dispatchers.IO)
                    .also { log.info("Finished processing all messages read from inbox") }
            }
        }
            .onSuccess {
                val timeToCompletion = Duration.between(timeStart, Instant.now())
                val throughputPerMinute = messages.count() / (timeToCompletion.toMillis().toDouble() / 1000 / 60)
                log.info(
                    Markers.appendEntries(mapOf(Pair("MailReaderTPM", throughputPerMinute))),
                    "${messages.count()} messages processed in ${timeToCompletion.toKotlinDuration()},($throughputPerMinute tpm)"
                )
            }
            .onFailure {
                log.error(it.message, it)
                log.info(it.localizedMessage)
            }

        return messages
    }

    private suspend fun postEbmsMessages(ebms: Ebms, message: EmailMsg): Result<HttpResponse> {
        return runCatching {
            if (message.parts.size == 1 && message.parts.first().headers.isEmpty()) {
                httpClient.postEbmsMessageSinglePart(ebms, message)
            } else {
                httpClient.postEbmsMessageMultiPart(ebms, message)
            }
        }
            .onFailure {
                log.error("Error posting EBMS message: ${it.message}", it)
            }
    }
}
