package no.nav.emottak.ebms.kafka

import io.github.nomisRev.kafka.receiver.AutoOffsetReset
import io.github.nomisRev.kafka.receiver.KafkaReceiver
import io.github.nomisRev.kafka.receiver.ReceiverSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import no.nav.emottak.config.Config
import no.nav.emottak.ebms.service.FagmeldingResponseService
import no.nav.emottak.fellesformat.unmarshal
import no.nav.emottak.util.EventRegistrationService
import no.nav.emottak.utils.common.model.SendInResponse
import no.nav.emottak.utils.common.parseOrGenerateUuid
import no.nav.emottak.utils.config.Kafka
import no.nav.emottak.utils.config.toProperties
import no.nav.emottak.utils.kafka.model.EventType
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

private val log = LoggerFactory.getLogger("no.nav.emottak.ebms.kafka.EbmsOutFellesformatReceiver")

// Denne lytteren leser Fellesformat-responsmeldinger (XML) og konverterer til SendInResponse (json) som legges på EbmsOutPayload-topic (til ebms-async).
fun CoroutineScope.launchEbmsOutFellesformatReceiver(
    config: Config,
    eventRegistrationService: EventRegistrationService,
    ebmsOutPayloadProducer: EbmsOutPayloadProducer
) {
    if (config.ebmsOutFellesformatReceiver.active) {
        launch(Dispatchers.IO) {
            startEbmsOutFellesformatReceiver(
                config.ebmsOutFellesformatReceiver.topic,
                config.kafka,
                ebmsOutPayloadProducer,
                eventRegistrationService
            )
        }
    }
}

private suspend fun startEbmsOutFellesformatReceiver(
    topic: String,
    kafka: Kafka,
    ebmsOutPayloadProducer: EbmsOutPayloadProducer,
    eventRegistrationService: EventRegistrationService
) {
    log.info("Starting EbmsOutFellesformat receiver on topic: {} with groupId: {} bootstrapServers: {} autoOffsetReset: Earliest", topic, kafka.groupId, kafka.bootstrapServers)
    val receiverSettings = ReceiverSettings<String, ByteArray>(
        bootstrapServers = kafka.bootstrapServers,
        keyDeserializer = StringDeserializer(),
        valueDeserializer = ByteArrayDeserializer(),
        groupId = kafka.groupId,
        autoOffsetReset = AutoOffsetReset.Latest,
        pollTimeout = 1.seconds,
        properties = kafka.toProperties()
    )

    KafkaReceiver(receiverSettings)
        .receive(topic)
        .collect { record ->
            val recordKey = record.key()
            log.info(
                "EbmsOutFellesformat received message on topic: {} partition: {} offset: {} key: {} valueSize: {}",
                record.topic(),
                record.partition(),
                record.offset(),
                recordKey,
                record.value()?.size ?: 0
            )
            withContext(MDCContext(mapOf("record_key" to recordKey))) {
                try {
                    processMessage(recordKey, record.value(), ebmsOutPayloadProducer, eventRegistrationService)
                    record.offset.acknowledge()
                    log.debug(
                        "EbmsOutFellesformat acknowledged offset: {} on partition: {}",
                        record.offset(),
                        record.partition()
                    )
                } catch (e: Exception) {
                    log.error("Error processing EbmsOutFellesformat message", e)
                }
            }
        }
}

suspend fun processMessage(
    recordKey: String,
    payload: ByteArray,
    ebmsOutPayloadProducer: EbmsOutPayloadProducer,
    eventRegistrationService: EventRegistrationService
) {
    val fellesformat = unmarshal(payload.toString(Charsets.UTF_8), EIFellesformat::class.java)

    log.info("EbmsOutFellesformat processing message from fagsystem")

    val mdcData = mapOf(
        "record_key" to recordKey,
        "messageId" to fellesformat.mottakenhetBlokk.ediLoggId,
        "conversationId" to fellesformat.mottakenhetBlokk.ebXMLSamtaleId,
        "cpaId" to fellesformat.mottakenhetBlokk.partnerReferanse,
        "requestId" to fellesformat.mottakenhetBlokk.ediLoggId
    )

    return withContext(MDCContext(mdcData)) {
        val sendInResponse = FagmeldingResponseService.getResponse(fellesformat)
        eventRegistrationService.registerEvent(
            EventType.MESSAGE_RECEIVED_FROM_FAGSYSTEM,
            requestId = sendInResponse.requestId.parseOrGenerateUuid(),
            messageId = "",
            conversationId = sendInResponse.conversationId
        )
        eventRegistrationService.registerEventMessageDetails(sendInResponse)

        val json = Json.encodeToString<SendInResponse>(sendInResponse).toByteArray()
        ebmsOutPayloadProducer.send(sendInResponse.messageId, json)
        log.info("Message converted to SendInResponse with messageId: ${sendInResponse.messageId} and forwarded to EbmsOutPayload topic")
    }
}
