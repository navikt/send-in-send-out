package no.nav.emottak.publisher

import io.github.nomisRev.kafka.publisher.KafkaPublisher
import no.nav.emottak.configuration.Kafka
import no.nav.emottak.log
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.UUID

class MailPublisher(
    private val kafka: Kafka,
    private val kafkaPublisher: KafkaPublisher<String, ByteArray>
) {
    suspend fun publishMessage(referenceId: UUID, content: ByteArray) =
        kafkaPublisher.publishScope {
            publishCatching(toProducerRecord(referenceId, content))
        }
            .onSuccess { log.info("Published message with reference id: $referenceId") }
            .onFailure { log.error("Failed to publish message with reference id: $referenceId") }

    private fun toProducerRecord(referenceId: UUID, content: ByteArray) =
        ProducerRecord(
            kafka.topic,
            referenceId.toString(),
            content
        )
}
