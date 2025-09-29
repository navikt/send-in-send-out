package no.nav.emottak.frikort.rest

import kotlinx.datetime.LocalDate
import no.nav.emottak.fellesformat.asEIFellesFormatWithFrikort
import no.nav.emottak.util.toKotlinxInstant
import no.nav.emottak.validSendInHarBorgerFrikortRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FrikortConverterTest {

    @Test
    fun `Map from MsgHead to frikort model`() {
        val fagmelding = validSendInHarBorgerFrikortRequest.value.asEIFellesFormatWithFrikort()
        val convertedFagmelding = fagmelding.toFrikortsporringRequest()

        assertTrue(fagmelding.msgHead.document.isNotEmpty())

        assertEquals(fagmelding.msgHead.msgInfo.msgId, convertedFagmelding.eiFellesformat.msgHead!!.msgInfo.msgId)
        assertEquals(fagmelding.msgHead.msgInfo.genDate.toKotlinxInstant(), convertedFagmelding.eiFellesformat.msgHead.msgInfo.genDate)
        assertEquals(fagmelding.msgHead.msgInfo.type.v, convertedFagmelding.eiFellesformat.msgHead.msgInfo.type.v)
        assertEquals(fagmelding.msgHead.msgInfo.type.dn, convertedFagmelding.eiFellesformat.msgHead.msgInfo.type.dn)
        assertEquals(fagmelding.msgHead.msgInfo.sender.organisation.organisationName, convertedFagmelding.eiFellesformat.msgHead.msgInfo.sender.organization.organizationName)
        assertEquals(fagmelding.msgHead.msgInfo.receiver.organisation.organisationName, convertedFagmelding.eiFellesformat.msgHead.msgInfo.receiver.organization.organizationName)

        assertNotNull(convertedFagmelding.eiFellesformat.msgHead.documents?.first()?.refDoc?.content?.egenandelForesporselV2)
        assertEquals("12345678910", convertedFagmelding.eiFellesformat.msgHead.documents?.first()?.refDoc?.content?.egenandelForesporselV2?.harBorgerFrikort?.borgerFnr)
        assertEquals(LocalDate.parse("2023-11-20"), convertedFagmelding.eiFellesformat.msgHead.documents?.first()?.refDoc?.content?.egenandelForesporselV2?.harBorgerFrikort?.dato)
    }
}
