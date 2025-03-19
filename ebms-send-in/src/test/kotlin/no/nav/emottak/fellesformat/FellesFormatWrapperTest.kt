import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import no.nav.emottak.ebms.log
import no.nav.emottak.fellesformat.asEIFellesFormat
import no.nav.emottak.fellesformat.marshal
import no.nav.emottak.frikort.unmarshal
import no.nav.emottak.util.birthDay
import no.nav.emottak.util.marker
import no.nav.emottak.util.refParam
import no.nav.emottak.util.refParamFrikort
import no.nav.emottak.validSendInHarBorgerEgenandelfritakRequest
import no.nav.emottak.validSendInHarBorgerFrikortMengdeRequest
import no.nav.emottak.validSendInHarBorgerFrikortRequest
import no.nav.emottak.validSendInInntektforesporselRequest
import no.nav.emottak.validSendInPasientlisteRequest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FellesFormatWrapperTest {

    @Test
    fun `Wrap Pasientlisteforesporsel in Fellesformat`() {
        val sendInRequest = validSendInPasientlisteRequest.value
        val fellesFormat = sendInRequest.asEIFellesFormat()
        val refParam = refParam(fellesFormat)
        assertEquals("170870", birthDay(refParam))
        log.info(sendInRequest.marker(), "refParam ${birthDay(refParam)}")
        Assertions.assertEquals(fellesFormat.mottakenhetBlokk.ebService, sendInRequest.addressing.service)
        log.info(marshal(fellesFormat))
    }

    @Test
    fun `Wrap HarBorgerFrikort in Fellesformat`() {
        val sendInRequest = validSendInHarBorgerFrikortRequest.value
        val fellesFormat = sendInRequest.asEIFellesFormat()
        val payload = sendInRequest.payload
        val msgHead = unmarshal(String(payload), MsgHead::class.java)

        val action = msgHead.document.map { doc -> doc.refDoc.content.any }.first().first()
        val refParam = refParamFrikort(action)
        assertEquals("123456", birthDay(refParam))
        log.info(sendInRequest.marker(), "refParam ${birthDay(refParam)}")
        Assertions.assertEquals(fellesFormat.mottakenhetBlokk.ebService, sendInRequest.addressing.service)
        log.info(marshal(fellesFormat))
    }

    @Test
    fun `Wrap HarBorgerEgenadelMengde in Fellesformat`() {
        val sendInRequest = validSendInHarBorgerFrikortMengdeRequest.value
        val fellesFormat = sendInRequest.asEIFellesFormat()
        val payload = sendInRequest.payload
        val msgHead = unmarshal(String(payload), MsgHead::class.java)

        val action = msgHead.document.map { doc -> doc.refDoc.content.any }.first().first()
        val refParam = refParamFrikort(action)
        assertEquals("2", refParam)
        log.info(sendInRequest.marker(), "refParam $refParam")
        Assertions.assertEquals(fellesFormat.mottakenhetBlokk.ebService, sendInRequest.addressing.service)
        log.info(marshal(fellesFormat))
    }

    @Test
    fun `Wrap HarBorgerEgenadelFritak in Fellesformat`() {
        val sendInRequest = validSendInHarBorgerEgenandelfritakRequest.value
        val fellesFormat = sendInRequest.asEIFellesFormat()
        val payload = sendInRequest.payload
        val msgHead = unmarshal(String(payload), MsgHead::class.java)

        val action = msgHead.document.map { doc -> doc.refDoc.content.any }.first().first()
        val refParam = refParamFrikort(action)
        assertEquals("123456", birthDay(refParam))
        log.info(sendInRequest.marker(), "refParam ${birthDay(refParam)}")
        Assertions.assertEquals(fellesFormat.mottakenhetBlokk.ebService, sendInRequest.addressing.service)
        log.info(marshal(fellesFormat))
    }

    @Test
    fun `Wrap Inntektsforesporsel in Fellesformat`() {
        val sendInRequest = validSendInInntektforesporselRequest.value
        val fellesFormat = sendInRequest.asEIFellesFormat()
        val refParam = refParam(fellesFormat)
        assertEquals("******", birthDay(refParam))
        log.info(sendInRequest.marker(), "refParam ${birthDay(refParam)}")
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
