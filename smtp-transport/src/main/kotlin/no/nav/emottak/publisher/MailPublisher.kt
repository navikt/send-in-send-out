package no.nav.emottak.publisher

import io.github.nomisRev.kafka.publisher.PublisherSettings
import io.github.nomisRev.kafka.publisher.produce
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import no.nav.emottak.configuration.Config
import no.nav.emottak.log
import no.nav.emottak.smtp.EmailMsg
import no.nav.emottak.util.getFirstPartAsBytes
import no.nav.emottak.util.getLastPartsAsPayloads
import no.nav.emottak.util.getMessageIdMultiPart
import no.nav.emottak.util.getMessageIdSinglePart
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata

class MailPublisher(
    private val config: Config,
    private val publisherSettings: PublisherSettings<String, ByteArray>
) {
    suspend fun publishMessages(messages: Flow<EmailMsg>) = messages
        .map { message ->
            when (message.parts.size > 1) {
                true -> processMultiPartMessage(message)
                else -> processSinglePartMessage(message)
            }
        }
        .produce(publisherSettings)
        .flowOn(Dispatchers.IO)
        .collect(::processResult)

    private fun processMultiPartMessage(message: EmailMsg): ProducerRecord<String, ByteArray> {
        // TDB: store payload in database
        val payloads = message.getLastPartsAsPayloads()
        payloads.forEach {
            log.info("Stored payload with message id ${it.messageId} and content type ${it.contentType}")
        }

        return ProducerRecord<String, ByteArray>(
            config.kafka.topic,
            message.getMessageIdMultiPart(),
            message.getFirstPartAsBytes()
        )
    }

    private fun processSinglePartMessage(message: EmailMsg): ProducerRecord<String, ByteArray> =
        ProducerRecord(
            config.kafka.topic,
            message.getMessageIdSinglePart(),
            message.getFirstPartAsBytes()
        )

    private fun processResult(metadata: Result<RecordMetadata>) =
        metadata
            .onSuccess { log.info("Partition: ${it.partition()}, offset: ${it.offset()}") }
            .onFailure { log.error("Failed to publish: $it") }
}
