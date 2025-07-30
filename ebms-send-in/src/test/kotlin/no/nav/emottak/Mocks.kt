package no.nav.emottak

import no.nav.emottak.utils.common.model.Addressing
import no.nav.emottak.utils.common.model.EbmsProcessing
import no.nav.emottak.utils.common.model.Party
import no.nav.emottak.utils.common.model.PartyId
import no.nav.emottak.utils.common.model.SendInRequest
import kotlin.uuid.Uuid

val validSendInPasientlisteRequest = lazy {
    val fagmelding = ClassLoader.getSystemResourceAsStream("hentpasientliste/hentpasientliste-payload.xml")
    mockSendInRequest("PasientisteForesporsel", "HentPasientliste", fagmelding.readAllBytes(), "123456789")
}

val invalidPidSendInPasientlisteRequest = lazy {
    val fagmelding = ClassLoader.getSystemResourceAsStream("hentpasientliste/hentpasientliste-payload-invalidPid.xml")
    mockSendInRequest("PasientisteForesporsel", "HentPasientliste", fagmelding.readAllBytes(), "11223312345")
}

val validSendInHarBorgerFrikortRequest = lazy {
    val fagmelding = ClassLoader.getSystemResourceAsStream("frikort/EgenandelForesporsel_HarBorgerFrikortRequest.xml")
    mockSendInRequest("HarBorgerFrikort", "EgenandelForesporsel", fagmelding.readAllBytes(), "123456789")
}

val validSendInHarBorgerEgenandelfritakRequest = lazy {
    val fagmelding = ClassLoader.getSystemResourceAsStream("frikort/EgenandelForesporsel_HarBorgerEgenandelFritakRequest.xml")
    mockSendInRequest("HarBorgerEgenandelFritak", "EgenandelForesporsel", fagmelding.readAllBytes(), "123456789")
}

val validSendInHarBorgerFrikortMengdeRequest = lazy {
    val fagmelding = ClassLoader.getSystemResourceAsStream("frikort/EgenandelMengdeForesporsel_HarBorgerFrikortMengdeRequest.xml")
    mockSendInRequest("HarBorgerFrikortMengde", "EgenandelMengdeForesporsel", fagmelding.readAllBytes(), "123456789")
}

val validSendInInntektforesporselRequest = lazy {
    val fagmelding = ClassLoader.getSystemResourceAsStream("inntektsforesporsel/inntektsforesporsel.xml")
    mockSendInRequest("Inntektsforesporsel", "Foresporsel", fagmelding.readAllBytes(), "123456789")
}

val validSendInInntektforesporselRequestWithENH = lazy {
    val fagmelding = ClassLoader.getSystemResourceAsStream("inntektsforesporsel/inntektsforesporsel_enh.xml")
    mockSendInRequest("Inntektsforesporsel", "Foresporsel", fagmelding.readAllBytes(), "123456789")
}

fun mockSendInRequest(service: String, action: String, payload: ByteArray, signedOf: String? = null) = SendInRequest(
    messageId = "321",
    conversationId = "321",
    payloadId = "123",
    addressing = mockAddressing(service, action),
    ebmsProcessing = EbmsProcessing(),
    cpaId = "dummyCpa",
    payload = payload,
    signedOf = signedOf,
    requestId = Uuid.random().toString()
)

fun mockAddressing(service: String, action: String): Addressing =
    Addressing(
        from = Party(
            partyId = listOf(PartyId("type", "value")),
            role = "dymmyRole"
        ),
        to = Party(
            partyId = listOf(PartyId("type", "value")),
            role = "dummyRole"
        ),
        service = service,
        action = action
    )
