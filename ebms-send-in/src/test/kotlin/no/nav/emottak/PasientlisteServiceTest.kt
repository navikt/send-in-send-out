package no.nav.emottak

import io.mockk.every
import io.mockk.mockkObject
import no.nav.emottak.fellesformat.asEIFellesFormat
import no.nav.emottak.fellesformat.unmarshal
import no.nav.emottak.pasientliste.PasientlisteClient
import no.nav.emottak.pasientliste.PasientlisteService
import no.nav.emottak.pasientliste.validator.PasientlisteValidator
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PasientlisteServiceTest {

    @Test
    fun `Should throw exception if SSN from certificate is missing`() {
        try {
            PasientlisteService.pasientlisteForesporsel(validSendInPasientlisteRequest.value.asEIFellesFormat())
        } catch (exception: RuntimeException) {
            Assertions.assertEquals(exception.message, PasientlisteValidator.CONFLICT_SIGNING_SSN)
        }
    }

    @Test
    fun `Should throw exception if SSN from certificate does not match SSN from message`() {
        val fellesformat = validSendInPasientlisteRequest.value.copy(
            signedOf = "12345678910"
        ).asEIFellesFormat()
        try {
            PasientlisteService.pasientlisteForesporsel(fellesformat)
        } catch (exception: RuntimeException) {
            Assertions.assertEquals(exception.message, PasientlisteValidator.CONFLICT_SIGNING_SSN)
        }
    }

    @Test
    fun `Should throw exception when senderFnr (SSN) matches request's signedOf, but signedOf is not a valid PID`() {
        try {
            PasientlisteService.pasientlisteForesporsel(invalidPidSendInPasientlisteRequest.value.asEIFellesFormat())
        } catch (exception: RuntimeException) {
            Assertions.assertEquals(exception.message, PasientlisteValidator.CONFLICT_INVALID_FNR)
        }
    }

    @Test
    fun `Should not throw exception when senderFnr (SSN) matches request's signedOf`() {
        val fellesformat = validSendInPasientlisteRequest.value.copy(
            signedOf = "17087000133"
        ).asEIFellesFormat()
        mockkObject(PasientlisteClient)
        every {
            PasientlisteClient.sendRequest(fellesformat)
        } answers {
            ClassLoader.getSystemResourceAsStream("hentpasientliste/pasientListeToktResponse.xml").readAllBytes().let {
                unmarshal(String(it), EIFellesformat::class.java)
            }
        }
        PasientlisteService.pasientlisteForesporsel(fellesformat)
    }
}
