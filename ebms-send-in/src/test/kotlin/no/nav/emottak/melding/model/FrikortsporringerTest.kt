package no.nav.emottak.melding.model

import no.nav.emottak.fellesformat.asEIFellesFormat
import no.nav.emottak.melding.model.FrikortsporringRequest.Companion.asFrikortsporringRequest
import no.nav.emottak.melding.model.MsgHead.Companion.toKithMsgHead
import no.nav.emottak.validSendInHarBorgerFrikortRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FrikortsporringerTest {

    @Test
    fun `Map from MsgHead to frikort model`() {
        val fagmelding = validSendInHarBorgerFrikortRequest.value.asEIFellesFormat()
        val convertedFagmelding = fagmelding.asFrikortsporringRequest()

        assertTrue(fagmelding.msgHead.document.isNotEmpty())

        assertEquals(fagmelding.msgHead.msgInfo.msgId, convertedFagmelding.eiFellesformat.msgHead.msgInfo.msgId)
        assertEquals(fagmelding.msgHead.msgInfo.genDate.toString(), convertedFagmelding.eiFellesformat.msgHead.msgInfo.genDate)
        assertEquals(fagmelding.msgHead.msgInfo.type.v, convertedFagmelding.eiFellesformat.msgHead.msgInfo.type.v)
        assertEquals(fagmelding.msgHead.msgInfo.type.dn, convertedFagmelding.eiFellesformat.msgHead.msgInfo.type.dn)
        assertEquals(fagmelding.msgHead.msgInfo.sender.organisation.organisationName, convertedFagmelding.eiFellesformat.msgHead.msgInfo.sender.organization.organizationName)
        assertEquals(fagmelding.msgHead.msgInfo.receiver.organisation.organisationName, convertedFagmelding.eiFellesformat.msgHead.msgInfo.receiver.organization.organizationName)

        assertNotNull(convertedFagmelding.eiFellesformat.msgHead?.documents?.first()?.refDoc?.content?.egenandelForesporselV2)
        assertEquals("12345678910", convertedFagmelding.eiFellesformat.msgHead.documents?.first()?.refDoc?.content?.egenandelForesporselV2?.harBorgerFrikort?.borgerFnr)
        assertEquals("2023-11-20", convertedFagmelding.eiFellesformat.msgHead.documents?.first()?.refDoc?.content?.egenandelForesporselV2?.harBorgerFrikort?.dato)
    }

    @Test
    fun `Map from frikort model to MsgHead`() {
        val fagmelding = validSendInHarBorgerFrikortRequest.value.asEIFellesFormat()
        val convertedFagmelding = fagmelding.asFrikortsporringRequest()

        val msgHead = convertedFagmelding.eiFellesformat.msgHead.toKithMsgHead()

        assertTrue(fagmelding.msgHead.document.isNotEmpty())

        assertEquals(msgHead.msgInfo.msgId, convertedFagmelding.eiFellesformat.msgHead.msgInfo.msgId)
        assertEquals(msgHead.msgInfo.genDate.toString(), convertedFagmelding.eiFellesformat.msgHead.msgInfo.genDate)
        assertEquals(msgHead.msgInfo.type.v, convertedFagmelding.eiFellesformat.msgHead.msgInfo.type.v)
        assertEquals(msgHead.msgInfo.type.dn, convertedFagmelding.eiFellesformat.msgHead.msgInfo.type.dn)
        assertEquals(msgHead.msgInfo.sender.organisation.organisationName, convertedFagmelding.eiFellesformat.msgHead.msgInfo.sender.organization.organizationName)
        assertEquals(msgHead.msgInfo.receiver.organisation.organisationName, convertedFagmelding.eiFellesformat.msgHead.msgInfo.receiver.organization.organizationName)

        assertEquals("12345678910", (msgHead?.document?.first()?.refDoc?.content?.any?.first() as no.kith.xmlstds.nav.egenandel._2016_06_10.EgenandelForesporselV2).harBorgerFrikort?.borgerFnr)
    }
}
