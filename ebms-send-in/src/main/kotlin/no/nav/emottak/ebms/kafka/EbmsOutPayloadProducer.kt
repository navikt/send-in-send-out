package no.nav.emottak.ebms.kafka

import no.nav.emottak.utils.config.Kafka
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.util.Properties
import java.util.concurrent.Future

private val log = LoggerFactory.getLogger("no.nav.emottak.ebms.kafka.EbmsOutPayloadProducer")

class EbmsOutPayloadProducer(
    private val topic: String,
    kafka: Kafka
) : Closeable {
    private val producer: KafkaProducer<String, String>

    init {
        val producerProps = Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer::class.java.name)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer::class.java.name)
            put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
        }
        producer = KafkaProducer(producerProps)
    }

    fun send(key: String?, value: String): Future<*> {
        log.info("Sending message to topic $topic")
        val record = ProducerRecord(topic, key, value)
        return producer.send(record)
    }

    override fun close() {
        producer.close()
    }
}