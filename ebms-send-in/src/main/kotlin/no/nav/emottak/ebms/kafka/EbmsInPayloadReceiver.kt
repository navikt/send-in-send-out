package no.nav.emottak.ebms.kafka

import io.github.nomisRev.kafka.receiver.AutoOffsetReset
import io.github.nomisRev.kafka.receiver.KafkaReceiver
import io.github.nomisRev.kafka.receiver.ReceiverSettings
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import no.nav.emottak.config.Config
import no.nav.emottak.ebms.service.FagmeldingService
import no.nav.emottak.util.EventRegistrationService
import no.nav.emottak.utils.common.model.SendInRequest
import no.nav.emottak.utils.common.model.SendInResponse
import no.nav.emottak.utils.common.parseOrGenerateUuid
import no.nav.emottak.utils.config.Kafka
import no.nav.emottak.utils.config.toProperties
import no.nav.emottak.utils.kafka.model.EventType
import no.nav.emottak.utils.serialization.toEventDataJson
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

private val log = LoggerFactory.getLogger("no.nav.emottak.ebms.kafka.EbmsInPayloadReceiver")

fun CoroutineScope.launchEbmsInPayloadReceiver(
    config: Config,
    eventRegistrationService: EventRegistrationService,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    outPayloadProducer: EbmsOutPayloadProducer
) {
    if (config.ebmsInPayloadReceiver.active) {
        launch(Dispatchers.IO) {
            startEbmsInPayloadReceiver(
                config.ebmsInPayloadReceiver.topic,
                config.kafka,
                eventRegistrationService,
                prometheusMeterRegistry,
                outPayloadProducer
            )
        }
    }
}

private suspend fun startEbmsInPayloadReceiver(
    topic: String,
    kafka: Kafka,
    eventRegistrationService: EventRegistrationService,
    prometheusMeterRegistry: PrometheusMeterRegistry,
    outPayloadProducer: EbmsOutPayloadProducer
) {
    log.info("Starting EbmsInPayload receiver on topic: {} with groupId: {} bootstrapServers: {} autoOffsetReset: Earliest", topic, kafka.groupId, kafka.bootstrapServers)
    val receiverSettings = ReceiverSettings<String, ByteArray>(
        bootstrapServers = kafka.bootstrapServers,
        keyDeserializer = StringDeserializer(),
        valueDeserializer = ByteArrayDeserializer(),
        groupId = kafka.groupId,
        autoOffsetReset = AutoOffsetReset.Earliest,
        pollTimeout = 1.seconds,
        properties = kafka.toProperties()
    )

    KafkaReceiver(receiverSettings)
        .receive(topic)
        .collect { record ->
            val recordKey = record.key() ?: "null"
            log.info(
                "EbmsInPayload received message on topic: {} partition: {} offset: {} key: {} valueSize: {}",
                record.topic(),
                record.partition(),
                record.offset(),
                recordKey,
                record.value()?.size ?: 0
            )
            withContext(MDCContext(mapOf("record_key" to recordKey))) {
                runCatching {
                    processMessage(recordKey, record.value(), eventRegistrationService, prometheusMeterRegistry)
                        ?.let { responseBody ->
                            outPayloadProducer.send(record.key(), responseBody.toByteArray())
                        }
                }.onFailure {
                    log.error("Error processing EbmsInPayload message", it)
                }
                record.offset.acknowledge()
                log.debug("EbmsInPayload acknowledged offset: {} on partition: {}", record.offset(), record.partition())
            }
        }
}

private suspend fun processMessage(
    recordKey: String,
    payload: ByteArray,
    eventRegistrationService: EventRegistrationService,
    prometheusMeterRegistry: PrometheusMeterRegistry
): String? {
    val sendInRequest = Json.decodeFromString<SendInRequest>(payload.decodeToString())
    log.info("EbmsInPayload ${sendInRequest.payloadId} processing message")

    val mdcData = mapOf(
        "record_key" to recordKey,
        "messageId" to sendInRequest.messageId,
        "conversationId" to sendInRequest.conversationId,
        "cpaId" to sendInRequest.cpaId,
        "requestId" to sendInRequest.requestId
    )

    return withContext(MDCContext(mdcData)) {
        FagmeldingService.processRequest(
            sendInRequest,
            prometheusMeterRegistry,
            eventRegistrationService
        ).fold(
            { error ->
                log.error("EbmsInPayload ${sendInRequest.payloadId} forwarding failed", error)
                eventRegistrationService.registerEvent(
                    EventType.ERROR_WHILE_SENDING_MESSAGE_TO_FAGSYSTEM,
                    sendInRequest.requestId.parseOrGenerateUuid(),
                    sendInRequest.messageId,
                    Exception(error).toEventDataJson()
                )
                null
            },
            { response ->
                log.info("EbmsInPayload ${sendInRequest.payloadId} forwarding complete, returning response")
                Json.encodeToString(SendInResponse.serializer(), response)
            }
        )
    }
}
