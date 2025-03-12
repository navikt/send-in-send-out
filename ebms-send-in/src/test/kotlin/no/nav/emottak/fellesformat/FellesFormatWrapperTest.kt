import no.nav.emottak.ebms.log
import no.nav.emottak.fellesformat.marshal
import no.nav.emottak.fellesformat.wrapMessageInEIFellesFormat
import no.nav.emottak.util.birthDay
import no.nav.emottak.util.marker
import no.nav.emottak.util.refParam
import no.nav.emottak.validSendInHarBorgerEgenandelfritakRequest
import no.nav.emottak.validSendInHarBorgerFrikortMengdeRequest
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
        val refParam = refParam(fellesFormat)
        Assertions.assertEquals(fellesFormat.mottakenhetBlokk.partnerReferanse, sendInRequest.cpaId)
        log.info(sendInRequest.marker(), "refParam ${birthDay(refParam)}")
        Assertions.assertEquals(fellesFormat.mottakenhetBlokk.partnerReferanse, sendInRequest.cpaId)
        log.info(marshal(fellesFormat))
    }

    @Test
    fun wrapMessageInHarBorgerEgenandelFritakREIFellesFormat() {
        68
        val sendInRequest = validSendInHarBorgerEgenandelfritakRequest.value
        val fellesFormat = wrapMessageInEIFellesFormat(sendInRequest)
        val refParam = refParam(fellesFormat)
        Assertions.assertEquals(fellesFormat.mottakenhetBlokk.partnerReferanse, sendInRequest.cpaId)
        log.info(sendInRequest.marker(), "refParam ${birthDay(refParam)}")
        log.info(marshal(fellesFormat))
    }

    @Test
    fun wrapMessageInHarBorgerFrikortREIFellesFormat() {
        val sendInRequest = validSendInHarBorgerFrikortRequest.value
        val fellesFormat = wrapMessageInEIFellesFormat(sendInRequest)
        val refParam = refParam(fellesFormat)
        Assertions.assertEquals(fellesFormat.mottakenhetBlokk.partnerReferanse, sendInRequest.cpaId)
        log.info(sendInRequest.marker(), "refParam ${birthDay(refParam)}")
        log.info(marshal(fellesFormat))
    }

    @Test
    fun wrapMessageInHarBorgerFrikortMengdeREIFellesFormat() {
        val sendInRequest = validSendInHarBorgerFrikortMengdeRequest.value
        val fellesFormat = wrapMessageInEIFellesFormat(sendInRequest)
        val refParam = refParam(fellesFormat)
        Assertions.assertEquals(fellesFormat.mottakenhetBlokk.partnerReferanse, sendInRequest.cpaId)
        log.info(sendInRequest.marker(), "refParam $refParam")
        log.info(marshal(fellesFormat))
    }

    @Test
    fun wrapMessageInInntektforesporselEIFellesFormat() {
        // val sendInRequest = validSendInInntektforesporselRequestWithENH.value
        val sendInRequest = validSendInInntektforesporselRequest.value
        val fellesFormat = wrapMessageInEIFellesFormat(sendInRequest)
        val refParam = refParam(fellesFormat)
        Assertions.assertEquals(fellesFormat.mottakenhetBlokk.partnerReferanse, sendInRequest.cpaId)
        log.info(sendInRequest.marker(), "refParam ${birthDay(refParam)}")
        log.info(marshal(fellesFormat))
    }
}
