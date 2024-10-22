package no.nav.emottak.fellesformat

import no.nav.emottak.ebms.log
import no.nav.emottak.validSendInHarBorgerFrikortRequest
import no.nav.emottak.validSendInInntektforesporselRequest
import no.nav.emottak.validSendInPasientlisteRequest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class FellesFormatWrapperTest {

    @Test
    fun wrapMessageInPasientlisteForesporselEIFellesFormat() {
        val sendInRequest = validSendInPasientlisteRequest.value
        val fellesFormat = wrapMessageInEIFellesFormat(sendInRequest)
        Assertions.assertEquals(fellesFormat.mottakenhetBlokk.partnerReferanse, sendInRequest.cpaId)
        log.info(marshal(fellesFormat))
    }

    @Test
    fun wrapMessageInHarBorgerFrikortREIFellesFormat() {
        val sendInRequest = validSendInHarBorgerFrikortRequest.value
        val fellesFormat = wrapMessageInEIFellesFormat(sendInRequest)
        Assertions.assertEquals(fellesFormat.mottakenhetBlokk.partnerReferanse, sendInRequest.cpaId)
        log.info(marshal(fellesFormat))
    }

    @Test
    fun wrapMessageInInntektforesporselEIFellesFormat() {
        val sendInRequest = validSendInInntektforesporselRequest.value
        val fellesFormat = wrapMessageInEIFellesFormat(sendInRequest)
        Assertions.assertEquals(fellesFormat.mottakenhetBlokk.partnerReferanse, sendInRequest.cpaId)
        log.info(marshal(fellesFormat))
    }
}
