import no.nav.emottak.ebms.log
import no.nav.emottak.fellesformat.marshal
import no.nav.emottak.fellesformat.wrapMessageInEIFellesFormat
import no.nav.emottak.validSendInHarBorgerFrikortRequest
import no.nav.emottak.validSendInInntektforesporselRequest
import no.nav.emottak.validSendInPasientlisteRequest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class FellesFormatWrapperTest {

    @Test
    fun `Wrap Pasientlisteforesporsel in Fellesformat`() {
        val sendInRequest = validSendInPasientlisteRequest.value
        val fellesFormat = wrapMessageInEIFellesFormat(sendInRequest)
        Assertions.assertEquals(fellesFormat.mottakenhetBlokk.ebService, sendInRequest.addressing.service)
        Assertions.assertEquals(fellesFormat.mottakenhetBlokk.partnerReferanse, sendInRequest.cpaId)
        log.info(marshal(fellesFormat))
    }

    @Test
    fun `Wrap HarBorgerFrikort in Fellesformat`() {
        val sendInRequest = validSendInHarBorgerFrikortRequest.value
        val fellesFormat = wrapMessageInEIFellesFormat(sendInRequest)
        Assertions.assertEquals(fellesFormat.mottakenhetBlokk.ebService, sendInRequest.addressing.service)
        Assertions.assertEquals(fellesFormat.mottakenhetBlokk.partnerReferanse, sendInRequest.cpaId)
        log.info(marshal(fellesFormat))
    }

    @Test
    fun `Wrap Inntektsforesporsel in Fellesformat`() {
        val sendInRequest = validSendInInntektforesporselRequest.value
        val fellesFormat = wrapMessageInEIFellesFormat(sendInRequest)
        Assertions.assertEquals(fellesFormat.mottakenhetBlokk.ebService, sendInRequest.addressing.service)
        Assertions.assertEquals(fellesFormat.mottakenhetBlokk.partnerReferanse, sendInRequest.cpaId)
        log.info(marshal(fellesFormat))
    }

    @Test
    fun `Validate SSN from request is added to Fellesformat`() {
        val sendInRequest = validSendInPasientlisteRequest.value
        val fellesFormat = wrapMessageInEIFellesFormat(sendInRequest)
        Assertions.assertEquals(fellesFormat.mottakenhetBlokk.avsenderFnrFraDigSignatur, sendInRequest.signedOf)
    }
}
