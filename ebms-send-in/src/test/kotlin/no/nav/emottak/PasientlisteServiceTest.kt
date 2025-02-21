package no.nav.emottak

import io.mockk.every
import io.mockk.mockkObject
import no.nav.emottak.fellesformat.unmarshal
import no.nav.emottak.pasientliste.PasientlisteClient
import no.nav.emottak.pasientliste.PasientlisteService
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class PasientlisteServiceTest {

    @Test
    fun `Should throw exception if SSN from certificate does not match SSN from message`() {
        val sendInRequest = validSendInPasientlisteRequest.value
        try {
            val responseRequestId = Uuid.random().toString()
            PasientlisteService.pasientlisteForesporsel(sendInRequest, responseRequestId)
        } catch (exception: RuntimeException) {
            Assertions.assertEquals(exception.message, PasientlisteService.CONFLICT_SIGNING_SSN)
        }
    }

    @Test
    fun `Should not throw exception when senderFnr (SSN) matches request's signedOf`() {
        val sendIndRequest = validSendInPasientlisteRequest.value
        val fnrFraFagmeldingen = "17087000133"
        mockkObject(PasientlisteClient)
        every {
            PasientlisteClient.sendRequest(any())
        } answers {
            ClassLoader.getSystemResourceAsStream("hentpasientliste/pasientListeToktResponse.xml").readAllBytes().let {
                unmarshal(String(it), EIFellesformat::class.java)
            }
        }
        val responseRequestId = Uuid.random().toString()
        PasientlisteService.pasientlisteForesporsel(sendIndRequest.copy(signedOf = fnrFraFagmeldingen), responseRequestId)
    }
}
