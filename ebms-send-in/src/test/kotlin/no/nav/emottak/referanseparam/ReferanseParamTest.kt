package no.nav.emottak.referanseparam

import no.nav.emottak.fellesformat.asEIFellesFormat
import no.nav.emottak.util.extractReferenceParameter
import no.nav.emottak.validSendInHarBorgerEgenandelFritakRequest
import no.nav.emottak.validSendInHarBorgerFrikortMengdeRequest
import no.nav.emottak.validSendInHarBorgerFrikortRequest
import no.nav.emottak.validSendInInntektforesporselRequest
import no.nav.emottak.validSendInPasientlisteRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReferanseParamTest {

    @Test
    fun `Wrap Pasientlisteforesporsel in Fellesformat`() {
        val sendInRequest = validSendInPasientlisteRequest.value
        val fellesFormat = sendInRequest.asEIFellesFormat()
        val refParam = fellesFormat.extractReferenceParameter()
        assertEquals("170870", refParam)
        assertEquals(fellesFormat.mottakenhetBlokk.ebService, sendInRequest.addressing.service)
    }

    @Test
    fun `Wrap HarBorgerFrikort in Fellesformat`() {
        val sendInRequest = validSendInHarBorgerFrikortRequest.value
        val fellesFormat = sendInRequest.asEIFellesFormat()
        val refParam = fellesFormat.extractReferenceParameter()
        assertEquals("123456", refParam)
        assertEquals(fellesFormat.mottakenhetBlokk.ebService, sendInRequest.addressing.service)
    }

    @Test
    fun `Wrap HarBorgerEgenadelMengde in Fellesformat`() {
        val sendInRequest = validSendInHarBorgerFrikortMengdeRequest.value
        val fellesFormat = sendInRequest.asEIFellesFormat()
        val refParam = fellesFormat.extractReferenceParameter()
        assertEquals("2", refParam)
        assertEquals(fellesFormat.mottakenhetBlokk.ebService, sendInRequest.addressing.service)
    }

    @Test
    fun `Wrap HarBorgerEgenadelFritak in Fellesformat`() {
        val sendInRequest = validSendInHarBorgerEgenandelFritakRequest.value
        val fellesFormat = sendInRequest.asEIFellesFormat()
        val refParam = fellesFormat.extractReferenceParameter()
        assertEquals("123456", refParam)
        assertEquals(fellesFormat.mottakenhetBlokk.ebService, sendInRequest.addressing.service)
    }

    @Test
    fun `Wrap Inntektsforesporsel in Fellesformat`() {
        val sendInRequest = validSendInInntektforesporselRequest.value
        val fellesFormat = sendInRequest.asEIFellesFormat()
        val refParam = fellesFormat.extractReferenceParameter()
        assertEquals("******", refParam)
        assertEquals(fellesFormat.mottakenhetBlokk.ebService, sendInRequest.addressing.service)
    }
}
