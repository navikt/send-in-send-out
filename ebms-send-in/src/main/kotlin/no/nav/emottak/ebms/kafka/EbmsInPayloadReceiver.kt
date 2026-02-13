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
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import no.nav.emottak.config.Config
import no.nav.emottak.ebms.service.FagmeldingService
import no.nav.emottak.log
import no.nav.emottak.util.EventRegistrationService
import no.nav.emottak.utils.common.model.SendInRequest
import no.nav.emottak.utils.common.model.SendInResponse
import no.nav.emottak.utils.common.parseOrGenerateUuid
import no.nav.emottak.utils.config.Kafka
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

    KafkaReceiver(receiverSettings)
        .receive(topic)
        .map { record ->
            val mdcData = mapOf("record_key" to (record.key() ?: "null"))
            withContext(MDCContext(mdcData)) {
                runCatching {
                    processMessage(record.value(), eventRegistrationService, prometheusMeterRegistry)
                        ?.let { responseBody ->
                            // TODO: Send to intermediate destination (TBD)
                            log.info("Processed message, response ready for forwarding")
                        }
                }.onFailure {
                    log.error("Error processing EbmsInPayload message", it)
                }
                record.offset.acknowledge()
            }
        }
        .collect()
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
        FagmeldingService.processRequest(
            sendInRequest,
            prometheusMeterRegistry,
            eventRegistrationService
        ).fold(
            { error ->
                log.error("Payload ${sendInRequest.payloadId} forwarding failed", error)
                eventRegistrationService.registerEvent(
                    EventType.ERROR_WHILE_SENDING_MESSAGE_TO_FAGSYSTEM,
                    sendInRequest.requestId.parseOrGenerateUuid(),
                    sendInRequest.messageId,
                    Exception(error).toEventDataJson()
                )
                null
                // Bad Request
                /*
                call.respond(
                    HttpStatusCode.BadRequest,
                    error.localizedMessage ?: error.javaClass.simpleName
                 )
                 */
            },
            { response ->
                log.info("Payload ${sendInRequest.payloadId} forwarding complete, returning response")
                //call.respond(response)
                // send to embsoutpayload
                Json.encodeToString(SendInResponse.serializer(), response)
            }
        )
    }
}