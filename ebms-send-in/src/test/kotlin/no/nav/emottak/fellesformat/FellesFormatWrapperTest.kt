import no.nav.emottak.ebms.log
import no.nav.emottak.fellesformat.asEIFellesFormat
import no.nav.emottak.fellesformat.marshal
import no.nav.emottak.util.birthDay
import no.nav.emottak.util.marker
import no.nav.emottak.util.refParam
import no.nav.emottak.validSendInHarBorgerEgenandelfritakRequest
import no.nav.emottak.validSendInHarBorgerFrikortMengdeRequest
import no.nav.emottak.validSendInHarBorgerFrikortRequest
import no.nav.emottak.validSendInInntektforesporselRequest
import no.nav.emottak.validSendInPasientlisteRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FellesFormatWrapperTest {

    @Test
    fun wrapMessageInPasientlisteForesporselEIFellesFormat() {
        val sendInRequest = validSendInPasientlisteRequest.value.asEIFellesFormat()
        val refParam = refParam(sendInRequest)
        log.info(sendInRequest.marker(), "refParam ${birthDay(refParam)}")
        log.info(marshal(sendInRequest))
        assertEquals("170870", "${birthDay(refParam)}")
    }

    @Test
    fun wrapMessageInHarBorgerEgenandelFritakREIFellesFormat() {
        val sendInRequest = validSendInHarBorgerEgenandelfritakRequest.value.asEIFellesFormat()
        val refParam = refParam(sendInRequest)
        log.info(sendInRequest.marker(), "refParam ${birthDay(refParam)}")
        log.info(marshal(sendInRequest))
        assertEquals("311260", "${birthDay(refParam)}")
    }

    @Test
    fun wrapMessageInHarBorgerFrikortREIFellesFormat() {
        val sendInRequest = validSendInHarBorgerFrikortRequest.value.asEIFellesFormat()
        val refParam = refParam(sendInRequest)
        log.info(sendInRequest.marker(), "refParam ${birthDay(refParam)}")
        log.info(marshal(sendInRequest))
        assertEquals("010976", "${birthDay(refParam)}")
    }

    @Test
    fun wrapMessageInHarBorgerFrikortMengdeREIFellesFormat() {
        val sendInRequest = validSendInHarBorgerFrikortMengdeRequest.value.asEIFellesFormat()
        val refParam = refParam(sendInRequest)
        log.info(sendInRequest.marker(), "refParam $refParam")
        log.info(marshal(sendInRequest))
        assertEquals("4", "$refParam")
    }

    @Test
    fun wrapMessageInInntektforesporselEIFellesFormat() {
        val sendInRequest = validSendInInntektforesporselRequest.value.asEIFellesFormat()
        val refParam = refParam(sendInRequest)
        log.info(sendInRequest.marker(), "refParam ${birthDay(refParam)}")
        log.info(marshal(sendInRequest))
        assertEquals("221100", "${birthDay(refParam)}")
    }
}
