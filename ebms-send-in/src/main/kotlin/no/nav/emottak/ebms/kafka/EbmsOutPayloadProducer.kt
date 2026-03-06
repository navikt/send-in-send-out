package no.nav.emottak.ebms.kafka

import no.nav.emottak.utils.config.Kafka
import no.nav.emottak.utils.config.toProperties
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.util.Properties
import java.util.concurrent.Future

private val log = LoggerFactory.getLogger("no.nav.emottak.ebms.kafka.EbmsOutPayloadProducer")

class EbmsOutPayloadProducer(
    private val topic: String,
    kafka: Kafka
) : Closeable {
    private val producer: KafkaProducer<String, ByteArray>

    init {
        val producerProps = Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer::class.java.name)
            put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
            putAll(kafka.toProperties())
        }
        producer = KafkaProducer(producerProps)
    }

    fun send(key: String, payload: ByteArray, headers: List<Header> = emptyList()): Future<*> {
        log.info("EbmsOutPayloadProducer sending message to topic $topic")

        val record = ProducerRecord(topic, null, key, payload, headers)
        return producer.send(record) { metadata, exception ->
            if (exception != null) {
                log.error("EbmsOutPayloadProducer failed to send message to topic $topic with key $key", exception)
            }
        }
    }

    override fun close() {
        producer.close()
    }
}
