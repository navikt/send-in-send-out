package no.nav.emottak.ebms.kafka

import io.github.nomisRev.kafka.receiver.AutoOffsetReset
import io.github.nomisRev.kafka.receiver.KafkaReceiver
import io.github.nomisRev.kafka.receiver.ReceiverSettings
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.coroutines.slf4j.MDCContext
import no.nav.emottak.config.Config
import no.nav.emottak.util.EventRegistrationService
import no.nav.emottak.utils.common.model.SendInRequest
import no.nav.emottak.utils.config.Kafka
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import java.util.Properties
import kotlin.time.Duration.Companion.seconds

private val log = LoggerFactory.getLogger("no.nav.emottak.ebms.kafka.EbmsInPayloadConsumer")

fun CoroutineScope.launchEbmsInPayloadReceiver(
    config: Config,
    eventRegistrationService: EventRegistrationService,
    prometheusMeterRegistry: PrometheusMeterRegistry
) {
    if (config.ebmsInPayloadReceiver.active) {
        launch(Dispatchers.IO) {
            startEbmsInPayloadReceiver(
                config.ebmsInPayloadReceiver.topic,
                config.kafka,
                eventRegistrationService,
                prometheusMeterRegistry
            )
        }
    }
}

suspend fun startEbmsInPayloadReceiver(
    topic: String,
    kafka: Kafka,
    eventRegistrationService: EventRegistrationService,
    prometheusMeterRegistry: PrometheusMeterRegistry
) {
    log.info("Starting EbmsInPayload receiver on topic $topic")
    val receiverSettings = ReceiverSettings<String, ByteArray>(
        bootstrapServers = kafka.bootstrapServers,
        keyDeserializer = StringDeserializer(),
        valueDeserializer = ByteArrayDeserializer(),
        groupId = kafka.groupId,
        autoOffsetReset = AutoOffsetReset.Earliest,
        pollTimeout = 1.seconds
    )

    val producerProps = Properties().apply {
        put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers)
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name) // Request key is String
        put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true")
        // No transactional.id since we are not using transactions here yet, avoiding ID conflicts if not managed carefully
    }

    val producer = KafkaProducer<String, String>(producerProps)
    producer.use { producer ->
        KafkaReceiver(receiverSettings)
            .receive(topic)
            .map { record ->
                val mdcData = mapOf("record_key" to (record.key() ?: "null"))
                withContext(MDCContext(mdcData)) {
                    runCatching {
                        processMessage(record.value(), eventRegistrationService, prometheusMeterRegistry)
                            ?.let { responseBody ->
                                // Using the same key for response as request? Or SendInResponse ID?
                                // Using record.key() for now which is roughly RequestId or similar.
                                val outRecord = ProducerRecord("ebms.out.payload", record.key(), responseBody)
                                producer.send(outRecord)
                            }
                    }.onFailure {
                        log.error("Error processing EbmsInPayload message", it)
                    }
                    record.offset.acknowledge()
                }
            }
            .collect()
    }
}

private suspend fun processMessage(
    payload: ByteArray,
    eventRegistrationService: EventRegistrationService,
    prometheusMeterRegistry: PrometheusMeterRegistry
): String? {
    val jsonString = String(payload)
    val sendInRequest = Json.decodeFromString<SendInRequest>(jsonString)

    val mdcData = mapOf(
        "messageId" to sendInRequest.messageId,
        "conversationId" to sendInRequest.conversationId,
        "cpaId" to sendInRequest.cpaId,
        "requestId" to sendInRequest.requestId
    )

    return withContext(MDCContext(mdcData)) {
        log.info("Dummy processing enabled. Message with id ${sendInRequest.messageId} received.")
        "Dummy response"
    }
}
