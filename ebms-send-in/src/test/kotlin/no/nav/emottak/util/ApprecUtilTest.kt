package no.nav.emottak.util

import no.kith.xmlstds.apprec._2004_11_21.AppRec
import no.kith.xmlstds.msghead._2006_05_24.CS
import no.kith.xmlstds.msghead._2006_05_24.CV
import no.kith.xmlstds.msghead._2006_05_24.HealthcareProfessional
import no.kith.xmlstds.msghead._2006_05_24.Ident
import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import no.kith.xmlstds.msghead._2006_05_24.MsgInfo
import no.kith.xmlstds.msghead._2006_05_24.Organisation
import no.kith.xmlstds.msghead._2006_05_24.Sender
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.uuid.Uuid

class ApprecUtilTest {

    @Test
    fun `negative AppRec contains rejection status and exception message`() {
        val apprec = createApprec(msgHead(), Exception("Invalid XML"))

        assertEquals("2", apprec.status.v)
        assertEquals("Avvist", apprec.status.dn)
        val error = apprec.error.single()
        assertEquals("2.16.578.1.12.4.1.1.8221", error.s)
        assertEquals("T99", error.v)
        assertEquals("Annen feil på format", error.dn)
        assertEquals("Invalid XML", error.ot)
    }

    @Test
    fun `negative AppRec supports an exception without a message`() {
        val apprec = createApprec(msgHead(), Exception())

        assertEquals("2", apprec.status.v)
        assertEquals("Avvist", apprec.status.dn)
        assertEquals("2.16.578.1.12.4.1.1.8221", apprec.error.single().s)
        assertEquals("T99", apprec.error.single().v)
        assertEquals("Annen feil på format", apprec.error.single().dn)
        assertEquals("Ukjent feil", apprec.error.single().ot)
    }

    @Test
    fun `AppRec contains version message type unique ID and current generation date`() {
        val request = msgHead()
        val before = Instant.now().toEpochMilli()
        val first = createApprec(request, Exception("Rejected"))
        val second = createApprec(request, Exception("Rejected"))
        val after = Instant.now().toEpochMilli()

        assertEquals("1.0 2004-11-21", first.miGversion)
        assertEquals("APPREC", first.msgType.v)
        assertEquals("Applikasjonskvittering", first.msgType.dn)
        assertEquals(first.id, Uuid.parse(first.id).toString())
        assertNotEquals(first.id, second.id)
        assertTrue(first.genDate.toGregorianCalendar().toInstant().toEpochMilli() in before..after)
    }

    @Test
    fun `AppRec references the original message`() {
        val request = msgHead()
        val apprec = createApprec(request, Exception("Rejected"))

        assertEquals(request.msgInfo.msgId, apprec.originalMsgId.id)
        assertEquals(request.msgInfo.genDate, apprec.originalMsgId.issueDate)
        assertEquals(request.msgInfo.type.v, apprec.originalMsgId.msgType.v)
        assertEquals(request.msgInfo.type.dn, apprec.originalMsgId.msgType.dn)
    }

    @Test
    fun `AppRec sender is NAV electronic reception`() {
        val sender = createApprec(msgHead(), Exception("Rejected")).sender.hcp.inst

        assertEquals("NAV", sender.name)
        assertEquals("889640782", sender.id)
        assertEquals("ENH", sender.typeId.v)
        assertEquals("Organisasjonsnummeret i Enhetsregisteret", sender.typeId.dn)
        assertEquals("Elektronisk mottak", sender.dept.single().name)
    }

    @Test
    fun `receiver prefers organisation number over HER and local identifiers`() {
        val request = msgHead(
            ident("local-id", "LIN"),
            ident("her-id", "HER"),
            ident("organisation-number", "ENH")
        )
        val receiver = createApprec(request, Exception("Rejected")).receiver.hcp.inst

        assertEquals("Test organisation", receiver.name)
        assertEquals("organisation-number", receiver.id)
        assertEquals("ENH", receiver.typeId.v)
        assertEquals("Organisasjonsnummeret i Enhetsregisteret", receiver.typeId.dn)
        assertTrue(receiver.hcPerson.isEmpty())
    }

    @Test
    fun `receiver prefers HER identifier when organisation number is absent`() {
        val request = msgHead(ident("local-id", "LIN"), ident("her-id", "HER"))
        val receiver = createApprec(request, Exception("Rejected")).receiver.hcp.inst

        assertEquals("her-id", receiver.id)
        assertEquals("HER", receiver.typeId.v)
        assertEquals("Identifikator fra Helsetjenesteenhetsregisteret (HER-id)", receiver.typeId.dn)
    }

    @Test
    fun `receiver falls back to first local identifier`() {
        val request = msgHead(ident("first-id", "LIN"), ident("second-id", "LIN"))
        val receiver = createApprec(request, Exception("Rejected")).receiver.hcp.inst

        assertEquals("first-id", receiver.id)
        assertEquals("LIN", receiver.typeId.v)
        assertEquals("Lokal identifikator for institusjoner", receiver.typeId.dn)
    }

    @Test
    fun `receiver includes healthcare professional with preferred identifier and normalized name`() {
        val request = msgHead()
        request.msgInfo.sender.organisation.healthcareProfessional = HealthcareProfessional().apply {
            givenName = "  Test  "
            middleName = " Middle "
            familyName = " Person  "
            ident.add(ident("person-her-id", "HER"))
            ident.add(ident("person-enh-id", "ENH"))
        }
        val person = createApprec(request, Exception("Rejected")).receiver.hcp.inst.hcPerson.single()

        assertEquals("person-enh-id", person.id)
        assertEquals("ENH", person.typeId.v)
        assertEquals("Test Middle Person", person.name)
    }

    @Test
    fun `healthcare professional name omits missing middle name`() {
        val request = msgHead()
        request.msgInfo.sender.organisation.healthcareProfessional = HealthcareProfessional().apply {
            givenName = "Test"
            familyName = "Person"
            ident.add(ident("person-id", "HER"))
        }
        val person = createApprec(request, Exception("Rejected")).receiver.hcp.inst.hcPerson.single()

        assertEquals("Test Person", person.name)
        assertEquals("person-id", person.id)
        assertEquals("HER", person.typeId.v)
    }

    @Test
    fun `missing organisation identifiers fail explicitly`() {
        val request = msgHead()
        request.msgInfo.sender.organisation.ident.clear()

        val error = assertThrows(RuntimeException::class.java) {
            createApprec(request, Exception("Rejected"))
        }

        assertEquals("Ident liste tom, dette skal ikke skje!", error.message)
    }

    @Test
    fun `missing healthcare professional identifiers fail explicitly`() {
        val request = msgHead()
        request.msgInfo.sender.organisation.healthcareProfessional = HealthcareProfessional()

        val error = assertThrows(RuntimeException::class.java) {
            createApprec(request, Exception("Rejected"))
        }

        assertEquals("Ident liste tom, dette skal ikke skje!", error.message)
    }

    @Test
    fun `unsupported organisation identifier type fails explicitly`() {
        assertThrows(IllegalArgumentException::class.java) {
            createApprec(msgHead(ident("unknown-id", "UNKNOWN")), Exception("Rejected"))
        }
    }

    @Test
    fun `negative AppRec survives XML marshalling with special characters in error message`() {
        val message = "Invalid XML: <element> & \"value\""
        val apprec = createApprec(msgHead(), Exception(message))

        val xml = apprecMarshaller.marshalToByteArray(apprec).toString(Charsets.UTF_8)
        val restored = apprecMarshaller.unmarshal(xml, AppRec::class.java)

        assertEquals(apprec.id, restored.id)
        assertEquals(apprec.genDate, restored.genDate)
        assertEquals(apprec.miGversion, restored.miGversion)
        assertEquals("APPREC", restored.msgType.v)
        assertEquals("2", restored.status.v)
        assertEquals(message, restored.error.single().ot)
        assertEquals(apprec.originalMsgId.id, restored.originalMsgId.id)
        assertEquals(apprec.originalMsgId.issueDate, restored.originalMsgId.issueDate)
        assertEquals(apprec.receiver.hcp.inst.id, restored.receiver.hcp.inst.id)
        assertEquals(apprec.sender.hcp.inst.id, restored.sender.hcp.inst.id)
    }

    private fun msgHead(vararg identifiers: Ident = arrayOf(ident("organisation-number", "ENH"))): MsgHead =
        MsgHead().apply {
            msgInfo = MsgInfo().apply {
                msgId = "original-message-id"
                genDate = Instant.parse("2024-01-02T03:04:05Z").toXmlGregorianCalendar()
                type = CS().apply {
                    v = "EgenandelForesporsel"
                    dn = "Original message"
                }
                sender = Sender().apply {
                    organisation = Organisation().apply {
                        organisationName = "Test organisation"
                        ident.addAll(identifiers)
                    }
                }
            }
        }

    private fun ident(value: String, type: String): Ident = Ident().apply {
        id = value
        typeId = CV().apply { v = type }
    }
}
