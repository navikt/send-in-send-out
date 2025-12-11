package no.nav.emottak.fellesformat

import no.nav.emottak.log
import no.nav.emottak.validSendInHarBorgerFrikortRequest
import no.nav.emottak.validSendInInntektforesporselRequest
import no.nav.emottak.validSendInPasientlisteRequest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class FellesFormatWrapperTest {

    @Test
    fun `Wrap Pasientlisteforesporsel in Fellesformat`() {
        val sendInRequest = validSendInPasientlisteRequest.value
        val fellesFormat = sendInRequest.asEIFellesFormat()
        Assertions.assertEquals(fellesFormat.mottakenhetBlokk.ebService, sendInRequest.addressing.service)
        log.info(marshal(fellesFormat))
    }

    @Test
    fun `Wrap HarBorgerFrikort in Fellesformat`() {
        val sendInRequest = validSendInHarBorgerFrikortRequest.value
        val fellesFormat = sendInRequest.asEIFellesFormat()
        Assertions.assertEquals(fellesFormat.mottakenhetBlokk.ebService, sendInRequest.addressing.service)
        log.info(marshal(fellesFormat))
    }

    @Test
    fun `Wrap Inntektsforesporsel in Fellesformat`() {
        val sendInRequest = validSendInInntektforesporselRequest.value
        val fellesFormat = sendInRequest.asEIFellesFormat()
        Assertions.assertEquals(fellesFormat.mottakenhetBlokk.ebService, sendInRequest.addressing.service)
        log.info(marshal(fellesFormat))
    }

    @Test
    fun `Validate SSN from request is added to Fellesformat`() {
        val sendInRequest = validSendInPasientlisteRequest.value
        val fellesFormat = sendInRequest.asEIFellesFormat()
        Assertions.assertEquals(fellesFormat.mottakenhetBlokk.avsenderFnrFraDigSignatur, sendInRequest.signedOf)
    }
}
