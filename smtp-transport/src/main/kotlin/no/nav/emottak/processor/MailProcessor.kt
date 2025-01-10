package no.nav.emottak.processor

import arrow.core.Either.Right
import arrow.core.left
import arrow.core.raise.either
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import no.nav.emottak.Dependencies
import no.nav.emottak.configuration.Config
import no.nav.emottak.log
import no.nav.emottak.publisher.MailPublisher
import no.nav.emottak.repository.PayloadRepository
import no.nav.emottak.smtp.EmailMsg
import no.nav.emottak.smtp.MailReader
import no.nav.emottak.util.dropFirst
import no.nav.emottak.util.getContent
import no.nav.emottak.util.toPayloads
import java.util.UUID.randomUUID

class MailProcessor(
    private val config: Config,
    private val deps: Dependencies,
    private val mailPublisher: MailPublisher,
    private val payloadRepository: PayloadRepository
) {
    suspend fun processMessages() = coroutineScope {
        readMessages()
            .onEach(::processMessage)
            .flowOn(Dispatchers.IO)
            .launchIn(this)
    }

    private fun readMessages(): Flow<EmailMsg> =
        MailReader(config.mail, deps.store, false).use { reader ->
            val messageCount = reader.count()

            if (messageCount > 0) {
                log.info("Starting to read $messageCount messages from inbox")
                reader.readMailBatches(messageCount)
                    .asFlow()
                    .also { log.info("Finished reading all messages from inbox") }
            } else {
                log.info("No messages found in inbox")
                emptyFlow()
            }
        }

    private suspend fun processMessage(emailMsg: EmailMsg) {
        val referenceId = randomUUID()
        val payloads = emailMsg.toPayloads(referenceId)

        if (payloads.size > 1) {
            // Multipart message: Insert payloads (drop the first which will be published)
            when (val result = with(payloadRepository) { either { insert(payloads.dropFirst()) } }) {
                is Right -> mailPublisher.publishPayloadMessage(referenceId, emailMsg.getContent())
                else -> log.error("Could not insert payload: ${result.left()}")
            }
        } else {
            // Single-part message: Directly publish
            mailPublisher.publishSignalMessage(referenceId, emailMsg.getContent())
        }
    }
}
