package no.nav.emottak.ebms.service

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import no.helsedir.frikort.frikorttjenester.model.FrikortsporringResponse
import no.kith.xmlstds.apprec._2004_11_21.AppRec
import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import no.nav.emottak.LegemeldingRequestTest
import no.nav.emottak.SykmeldingRequestTest
import no.nav.emottak.TrekkopplysningRequestTest
import no.nav.emottak.fellesformat.asEIFellesFormat
import no.nav.emottak.frikort.egenandelMengdeForesporselXmlMarshaller
import no.nav.emottak.frikort.frikortsporringMengde
import no.nav.emottak.frikort.rest.postHarBorgerEgenandelfritak
import no.nav.emottak.frikort.rest.postHarBorgerFrikort
import no.nav.emottak.legemelding.LegeMeldingService
import no.nav.emottak.mockSendInRequest
import no.nav.emottak.sykmelding.SyfoMeldingService
import no.nav.emottak.trekkopplysning.TrekkopplysningService
import no.nav.emottak.utbetaling.UtbetalingClient
import no.nav.emottak.utbetaling.UtbetalingXmlMarshaller
import no.nav.emottak.util.EventRegistrationService
import no.nav.emottak.util.apprecMarshaller
import no.nav.emottak.utils.common.model.SendInRequest
import no.nav.emottak.utils.common.model.SendInResponse
import no.nav.emottak.utils.kafka.model.EventType
import no.nav.emottak.utils.serialization.LENIENT_JSON_PARSER
import no.nav.emottak.validSendInHarBorgerEgenandelFritakRequest
import no.nav.emottak.validSendInHarBorgerFrikortMengdeRequest
import no.nav.emottak.validSendInHarBorgerFrikortRequest
import no.nav.emottak.validSendInInntektforesporselRequest
import no.nav.tjeneste.ekstern.frikort.v1.types.FrikortsporringMengdeResponse
import org.apache.cxf.binding.soap.SoapFault
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.xml.namespace.QName
import kotlin.uuid.Uuid

class FagmeldingServiceTest {

    private val meterRegistry = SimpleMeterRegistry()
    private lateinit var eventRegistrationService: EventRegistrationService

    @BeforeEach
    fun setUp() {
        eventRegistrationService = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // ---------- Synchronous ----------

    @Test
    fun `Inntektsforesporsel is sent to UtbetalingClient and registers sent, details and received events in order`() = runBlocking {
        val sendInRequest = validSendInInntektforesporselRequest.value
        mockkObject(UtbetalingClient)
        every { UtbetalingClient.behandleInntektsforesporsel(any()) } answers {
            UtbetalingXmlMarshaller.unmarshal(firstArg<ByteArray>().toString(Charsets.UTF_8), MsgHead::class.java)
                .apply { msgInfo.type.v = "InntektInformasjon" }
        }

        val response = FagmeldingService.processRequestSynchronously(sendInRequest, meterRegistry, eventRegistrationService)
            .getOrNull()

        assertNotNull(response)
        assertResponseCorrelatesWith(sendInRequest, response!!)
        assertEquals(sendInRequest.addressing.replyTo("Inntektsforesporsel", "InntektInformasjon"), response.addressing)
        coVerify(exactly = 1) { UtbetalingClient.behandleInntektsforesporsel(sendInRequest.payload) }
        verifySyncSuccessEvents(sendInRequest, response)
    }

    @Test
    fun `Inntektsforesporsel failure returns Left and registers no events`() = runBlocking {
        val sendInRequest = validSendInInntektforesporselRequest.value
        mockkObject(UtbetalingClient)
        every { UtbetalingClient.behandleInntektsforesporsel(any()) } throws IllegalStateException("Utbetaling down")

        val result = FagmeldingService.processRequestSynchronously(sendInRequest, meterRegistry, eventRegistrationService)

        assertEquals("Utbetaling down", result.leftOrNull()?.message)
        verifyNoEventsRegistered()
    }

    @Test
    fun `HarBorgerFrikortMengde is sent via SOAP and registers reference, sent, details and received events in order`() = runBlocking {
        val sendInRequest = validSendInHarBorgerFrikortMengdeRequest.value
        mockkStatic("no.nav.emottak.frikort.FrikortClientKt")
        every { frikortsporringMengde(any()) } answers {
            FrikortsporringMengdeResponse().apply { eiFellesformat = firstArg() }
        }
        val referenceParameter = slot<String>()
        every { eventRegistrationService.registerReferenceParameter(sendInRequest, capture(referenceParameter)) } returns Unit

        val response = FagmeldingService.processRequestSynchronously(sendInRequest, meterRegistry, eventRegistrationService)
            .getOrNull()

        assertNotNull(response)
        assertResponseCorrelatesWith(sendInRequest, response!!)
        assertEquals(sendInRequest.addressing.replyTo("HarBorgerFrikortMengde", "EgenandelMengdeForesporsel"), response.addressing)
        assertEquals(
            sendInRequest.asEIFellesFormat().msgHead.msgInfo.msgId,
            egenandelMengdeForesporselXmlMarshaller.unmarshal(String(response.payload), MsgHead::class.java).msgInfo.msgId
        )
        coVerify(exactly = 1) { frikortsporringMengde(any()) }
        assertTrue(referenceParameter.isCaptured)
        coVerifyOrder {
            eventRegistrationService.registerReferenceParameter(sendInRequest, referenceParameter.captured)
            eventRegistrationService.registerEvent(
                EventType.MESSAGE_SENT_TO_FAGSYSTEM,
                Uuid.parse(sendInRequest.requestId),
                sendInRequest.messageId,
                any(),
                sendInRequest.conversationId
            )
        }
        verifySyncSuccessEvents(sendInRequest, response)
    }

    @Test
    fun `HarBorgerFrikortMengde SOAP client fault returns negative AppRec and registers details and received events`() = runBlocking {
        val sendInRequest = validSendInHarBorgerFrikortMengdeRequest.value
        mockkStatic("no.nav.emottak.frikort.FrikortClientKt")
        every { frikortsporringMengde(any()) } throws SoapFault("Client error", QName(SOAP_ENV_NS, "Client"))

        val response = FagmeldingService.processRequestSynchronously(sendInRequest, meterRegistry, eventRegistrationService)
            .getOrNull()

        assertNotNull(response)
        assertResponseCorrelatesWith(sendInRequest, response!!)
        assertEquals(sendInRequest.addressing.replyTo("HarBorgerFrikortMengde", "Avvisning"), response.addressing)
        assertNotNull(apprecMarshaller.unmarshal(String(response.payload), AppRec::class.java))
        coVerify(exactly = 1) { eventRegistrationService.registerReferenceParameter(sendInRequest, any()) }
        coVerify(exactly = 0) {
            eventRegistrationService.registerEvent(EventType.MESSAGE_SENT_TO_FAGSYSTEM, any(), any(), any(), any())
        }
        val details = slot<SendInResponse>()
        coVerify(exactly = 1) { eventRegistrationService.registerEventMessageDetails(capture(details)) }
        assertEquals(response, details.captured)
        coVerify(exactly = 1) {
            eventRegistrationService.registerEvent(
                EventType.MESSAGE_RECEIVED_FROM_FAGSYSTEM,
                Uuid.parse(response.requestId),
                "",
                any(),
                sendInRequest.conversationId
            )
        }
    }

    @Test
    fun `HarBorgerFrikortMengde SOAP server fault returns Left and registers no sent or received events`() = runBlocking {
        val sendInRequest = validSendInHarBorgerFrikortMengdeRequest.value
        mockkStatic("no.nav.emottak.frikort.FrikortClientKt")
        val fault = SoapFault("Server error", QName(SOAP_ENV_NS, "Server"))
        every { frikortsporringMengde(any()) } throws fault

        val result = FagmeldingService.processRequestSynchronously(sendInRequest, meterRegistry, eventRegistrationService)

        assertEquals(fault, result.leftOrNull())
        coVerify(exactly = 1) { eventRegistrationService.registerReferenceParameter(sendInRequest, any()) }
        coVerify(exactly = 0) { eventRegistrationService.registerEvent(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { eventRegistrationService.registerEventMessageDetails(any()) }
    }

    @Test
    fun `HarBorgerFrikort is sent via REST and registers no events`() = runBlocking {
        val sendInRequest = validSendInHarBorgerFrikortRequest.value
        mockkStatic("no.nav.emottak.frikort.rest.FrikortHttpClientKt")
        coEvery { postHarBorgerFrikort(any()) } returns
            frikortRestResponse("frikort/rest/EgenandelForesporselV2_HarBorgerFrikortResponse.json")

        val response = FagmeldingService.processRequestSynchronously(sendInRequest, meterRegistry, eventRegistrationService)
            .getOrNull()

        assertNotNull(response)
        assertResponseCorrelatesWith(sendInRequest, response!!)
        assertEquals("HarBorgerFrikort", response.addressing.service)
        coVerify(exactly = 1) { postHarBorgerFrikort(any()) }
        coVerify(exactly = 0) { postHarBorgerEgenandelfritak(any()) }
        verifyNoEventsRegistered()
    }

    @Test
    fun `HarBorgerEgenandelFritak is sent via REST and registers no events`() = runBlocking {
        val sendInRequest = validSendInHarBorgerEgenandelFritakRequest.value
        mockkStatic("no.nav.emottak.frikort.rest.FrikortHttpClientKt")
        coEvery { postHarBorgerEgenandelfritak(any()) } returns
            frikortRestResponse("frikort/rest/EgenandelForesporselV2_harBorgerEgenandelfritak_response.json")

        val response = FagmeldingService.processRequestSynchronously(sendInRequest, meterRegistry, eventRegistrationService)
            .getOrNull()

        assertNotNull(response)
        assertResponseCorrelatesWith(sendInRequest, response!!)
        assertEquals("HarBorgerEgenandelFritak", response.addressing.service)
        coVerify(exactly = 1) { postHarBorgerEgenandelfritak(any()) }
        coVerify(exactly = 0) { postHarBorgerFrikort(any()) }
        verifyNoEventsRegistered()
    }

    @Test
    fun `HarBorgerFrikort REST failure returns Left and registers no events`() = runBlocking {
        val sendInRequest = validSendInHarBorgerFrikortRequest.value
        mockkStatic("no.nav.emottak.frikort.rest.FrikortHttpClientKt")
        coEvery { postHarBorgerFrikort(any()) } throws RuntimeException("Frikort REST down")

        val result = FagmeldingService.processRequestSynchronously(sendInRequest, meterRegistry, eventRegistrationService)

        assertEquals("Frikort REST down", result.leftOrNull()?.message)
        verifyNoEventsRegistered()
    }

    @Test
    fun `Unsupported synchronous service returns NotImplementedError and registers no events`() = runBlocking {
        val sendInRequest = mockSendInRequest("UkjentTjeneste", "UkjentAction", "<dummy/>".toByteArray())

        val result = FagmeldingService.processRequestSynchronously(sendInRequest, meterRegistry, eventRegistrationService)

        assertInstanceOf(NotImplementedError::class.java, result.leftOrNull())
        verifyNoEventsRegistered()
    }

    // ---------- Asynchronous ----------

    @Test
    fun `Trekkopplysning is sent to MQ and registers MESSAGE_SENT_TO_FAGSYSTEM with queue name`() {
        val sendInRequest = trekkopplysningRequest()
        val trekkopplysningService = mockk<TrekkopplysningService>()
        every { trekkopplysningService.queue } returns "TREKK_QUEUE"
        every { trekkopplysningService.trekkopplysning(any(), any()) } returns Unit

        val result = processAsync(sendInRequest, trekkopplysningService = trekkopplysningService)

        assertTrue(result.isRight())
        coVerifyOrder {
            trekkopplysningService.trekkopplysning(any(), sendInRequest.payload)
            eventRegistrationService.registerMessageSentToFagsystem(sendInRequest, "TREKK_QUEUE")
        }
        coVerify(exactly = 0) { eventRegistrationService.registerReferenceParameter(any(), any()) }
    }

    @Test
    fun `Trekkopplysning MQ failure returns Left and does not register MESSAGE_SENT_TO_FAGSYSTEM`() {
        val sendInRequest = trekkopplysningRequest()
        val trekkopplysningService = mockk<TrekkopplysningService>()
        every { trekkopplysningService.queue } returns "TREKK_QUEUE"
        every { trekkopplysningService.trekkopplysning(any(), any()) } throws RuntimeException("MQ unavailable")

        val result = processAsync(sendInRequest, trekkopplysningService = trekkopplysningService)

        assertEquals("MQ unavailable", result.leftOrNull()?.message)
        coVerify(exactly = 0) { eventRegistrationService.registerMessageSentToFagsystem(any(), any()) }
    }

    @Test
    fun `Sykmelding is sent to MQ and registers reference parameter and MESSAGE_SENT_TO_FAGSYSTEM`() {
        val sendInRequest = mockSendInRequest(
            "Sykmelding",
            "Registrering",
            SykmeldingRequestTest().payloadFromExpectedXmlFile.toByteArray()
        )
        val syfoMeldingService = mockk<SyfoMeldingService>()
        every { syfoMeldingService.queue } returns "SYFO_QUEUE"
        every { syfoMeldingService.sykmelding(any(), any()) } returns Unit
        val referenceParameter = slot<String>()
        every { eventRegistrationService.registerReferenceParameter(sendInRequest, capture(referenceParameter)) } returns Unit

        val result = processAsync(sendInRequest, syfoMeldingService = syfoMeldingService)

        assertTrue(result.isRight())
        assertEquals("0770", referenceParameter.captured)
        coVerifyOrder {
            eventRegistrationService.registerReferenceParameter(sendInRequest, "0770")
            syfoMeldingService.sykmelding(any(), sendInRequest.payload)
            eventRegistrationService.registerMessageSentToFagsystem(sendInRequest, "SYFO_QUEUE")
        }
    }

    @Test
    fun `Legemelding is sent to MQ and registers reference parameter and MESSAGE_SENT_TO_FAGSYSTEM`() {
        val sendInRequest = legemeldingRequest()
        val legeMeldingService = mockk<LegeMeldingService>()
        every { legeMeldingService.queue } returns "PALE_QUEUE"
        every { legeMeldingService.legemelding(any(), any()) } returns Unit
        val referenceParameter = slot<String>()
        every { eventRegistrationService.registerReferenceParameter(sendInRequest, capture(referenceParameter)) } returns Unit

        val result = processAsync(sendInRequest, legeMeldingService = legeMeldingService)

        assertTrue(result.isRight())
        assertEquals("1234", referenceParameter.captured)
        coVerifyOrder {
            eventRegistrationService.registerReferenceParameter(sendInRequest, "1234")
            legeMeldingService.legemelding(any(), sendInRequest.payload)
            eventRegistrationService.registerMessageSentToFagsystem(sendInRequest, "PALE_QUEUE")
        }
    }

    @Test
    fun `Legemelding MQ failure returns Left and does not register MESSAGE_SENT_TO_FAGSYSTEM`() {
        val sendInRequest = legemeldingRequest()
        val legeMeldingService = mockk<LegeMeldingService>()
        every { legeMeldingService.queue } returns "PALE_QUEUE"
        every { legeMeldingService.legemelding(any(), any()) } throws RuntimeException("MQ unavailable")

        val result = processAsync(sendInRequest, legeMeldingService = legeMeldingService)

        assertEquals("MQ unavailable", result.leftOrNull()?.message)
        coVerify(exactly = 1) { eventRegistrationService.registerReferenceParameter(sendInRequest, "1234") }
        coVerify(exactly = 0) { eventRegistrationService.registerMessageSentToFagsystem(any(), any()) }
    }

    @Test
    fun `Unsupported asynchronous service returns UnsupportedAsyncServiceException and registers no events`() {
        val sendInRequest = mockSendInRequest("HarBorgerFrikort", "EgenandelForesporsel", "<dummy/>".toByteArray())
        val trekkopplysningService = mockk<TrekkopplysningService>()
        val syfoMeldingService = mockk<SyfoMeldingService>()
        val legeMeldingService = mockk<LegeMeldingService>()

        val result = processAsync(sendInRequest, trekkopplysningService, syfoMeldingService, legeMeldingService)

        assertInstanceOf(UnsupportedAsyncServiceException::class.java, result.leftOrNull())
        coVerify(exactly = 0) { trekkopplysningService.trekkopplysning(any(), any()) }
        coVerify(exactly = 0) { syfoMeldingService.sykmelding(any(), any()) }
        coVerify(exactly = 0) { legeMeldingService.legemelding(any(), any()) }
        verifyNoEventsRegistered()
    }

    // ---------- Helpers ----------

    private fun processAsync(
        sendInRequest: SendInRequest,
        trekkopplysningService: TrekkopplysningService = mockk(),
        syfoMeldingService: SyfoMeldingService = mockk(),
        legeMeldingService: LegeMeldingService = mockk()
    ) = FagmeldingService.processRequestAsynchronously(
        sendInRequest,
        meterRegistry,
        eventRegistrationService,
        trekkopplysningService,
        syfoMeldingService,
        legeMeldingService
    )

    private fun trekkopplysningRequest() = mockSendInRequest(
        "Trekkopplysning",
        "Trekkopplysning",
        TrekkopplysningRequestTest().payloadFromExpectedXmlFile.toByteArray()
    )

    private fun legemeldingRequest() = mockSendInRequest(
        "Legemelding",
        "Legemelding",
        LegemeldingRequestTest().payloadFromExpectedXmlFile.toByteArray()
    )

    private fun verifySyncSuccessEvents(sendInRequest: SendInRequest, response: SendInResponse) {
        val details = slot<SendInResponse>()
        val receivedRequestId = slot<Uuid>()
        coVerifyOrder {
            eventRegistrationService.registerEvent(
                EventType.MESSAGE_SENT_TO_FAGSYSTEM,
                Uuid.parse(sendInRequest.requestId),
                sendInRequest.messageId,
                any(),
                sendInRequest.conversationId
            )
            eventRegistrationService.registerEventMessageDetails(capture(details))
            eventRegistrationService.registerEvent(
                EventType.MESSAGE_RECEIVED_FROM_FAGSYSTEM,
                capture(receivedRequestId),
                "",
                any(),
                sendInRequest.conversationId
            )
        }
        coVerify(exactly = 1) {
            eventRegistrationService.registerEvent(EventType.MESSAGE_SENT_TO_FAGSYSTEM, any(), any(), any(), any())
        }
        coVerify(exactly = 1) {
            eventRegistrationService.registerEvent(EventType.MESSAGE_RECEIVED_FROM_FAGSYSTEM, any(), any(), any(), any())
        }
        coVerify(exactly = 0) {
            eventRegistrationService.registerEvent(EventType.ERROR_WHILE_SENDING_MESSAGE_TO_FAGSYSTEM, any(), any(), any(), any())
        }
        assertEquals(response, details.captured)
        assertEquals(Uuid.parse(response.requestId), receivedRequestId.captured)
    }

    private fun verifyNoEventsRegistered() {
        coVerify(exactly = 0) { eventRegistrationService.registerEvent(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { eventRegistrationService.registerEventMessageDetails(any()) }
        coVerify(exactly = 0) { eventRegistrationService.registerReferenceParameter(any(), any()) }
        coVerify(exactly = 0) { eventRegistrationService.registerMessageSentToFagsystem(any(), any()) }
    }

    private fun assertResponseCorrelatesWith(sendInRequest: SendInRequest, response: SendInResponse) {
        assertEquals(sendInRequest.messageId, response.refToMessageId)
        assertEquals(sendInRequest.conversationId, response.conversationId)
        assertEquals(sendInRequest.cpaId, response.cpaId)
        assertNotEquals(sendInRequest.messageId, response.messageId)
        assertNotEquals(sendInRequest.requestId, response.requestId)
        assertTrue(response.payload.isNotEmpty())
    }

    private fun frikortRestResponse(resource: String): FrikortsporringResponse =
        LENIENT_JSON_PARSER.decodeFromString(
            FrikortsporringResponse.serializer(),
            ClassLoader.getSystemResourceAsStream(resource)!!.readAllBytes().decodeToString()
        )

    private companion object {
        const val SOAP_ENV_NS = "http://schemas.xmlsoap.org/soap/envelope/"
    }
}
