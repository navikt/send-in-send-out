package no.nav.emottak.trekkopplysning

import no.kith.xmlstds.apprec._2004_11_21.AppRec
import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import no.nav.emottak.fellesformat.unmarshal
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TrekkopplysningMarshallerTest {

    private fun loadResource(filename: String): String =
        this::class.java.classLoader.getResourceAsStream("trekkopplysning/$filename")!!
            .readAllBytes().toString(Charsets.UTF_8)

    // kvittering contains a MsgHead inside EI_fellesformat

    @Test
    fun `kvittering - extracted MsgHead marshals to XML with correct namespace`() {
        val fellesformat = unmarshal(loadResource("trekkopplysning_kvittering.xml"), EIFellesformat::class.java)
        val msgHead = fellesformat.msgHead

        val xml = msgheadTrekkopplysningMarshaller.marshal(msgHead)

        assertTrue(xml.contains("http://www.kith.no/xmlstds/msghead/2006-05-24"))
        assertTrue(xml.contains("MsgHead"))
    }

    @Test
    fun `kvittering - round-trip marshal then unmarshal preserves MsgInfo fields`() {
        val fellesformat = unmarshal(loadResource("trekkopplysning_kvittering.xml"), EIFellesformat::class.java)
        val msgHead = fellesformat.msgHead

        val xml = msgheadTrekkopplysningMarshaller.marshal(msgHead)
        val restored = msgheadTrekkopplysningMarshaller.unmarshal(xml, MsgHead::class.java)

        assertNotNull(restored.msgInfo)
        assertEquals("f2f0f2f6-f0f4-f2f1-f1f3-f2f5f1f6f9f4", restored.msgInfo.msgId)
        assertEquals("INNRAPPORTERING_TREKK_RETUR", restored.msgInfo.type.v)
        assertEquals("v1.2 2006-05-24", restored.msgInfo.getMIGversion())
        assertEquals("NAV IKT", restored.msgInfo.sender.organisation.organisationName)
        assertEquals("TEST AS", restored.msgInfo.receiver.organisation.organisationName)
    }

    @Test
    fun `kvittering - marshalToByteArray produces non-empty result containing expected MsgId`() {
        val fellesformat = unmarshal(loadResource("trekkopplysning_kvittering.xml"), EIFellesformat::class.java)
        val msgHead = fellesformat.msgHead

        val bytes = msgheadTrekkopplysningMarshaller.marshalToByteArray(msgHead)

        assertTrue(bytes.isNotEmpty())
        assertTrue(String(bytes).contains("f2f0f2f6-f0f4-f2f1-f1f3-f2f5f1f6f9f4"))
    }

    @Test
    fun `print marshalled output for both files`() {
        val kvittering = unmarshal(loadResource("trekkopplysning_kvittering.xml"), EIFellesformat::class.java)
        val avvisning = unmarshal(loadResource("trekkopplysning_avvisning.xml"), EIFellesformat::class.java)

        println("=== kvittering (MsgHead) ===")
        println(msgheadTrekkopplysningMarshaller.marshal(kvittering.msgHead))

        println("=== avvisning (AppRec) via minimalTrekkopplysningAppRecMarshaller ===")
        println(String(apprecTrekkopplysningMarshaller.marshalToByteArray(avvisning.appRec)))
    }

    // avvisning contains an AppRec inside EI_fellesformat

    @Test
    fun `avvisning - extracted AppRec marshals to XML containing expected elements`() {
        val fellesformat = unmarshal(loadResource("trekkopplysning_avvisning.xml"), EIFellesformat::class.java)
        val appRec = fellesformat.appRec

        val xml = String(apprecTrekkopplysningMarshaller.marshalToByteArray(appRec))

        assertTrue(xml.contains("AppRec"))
        assertTrue(xml.contains("Avvist"))
    }

    @Test
    fun `avvisning - round-trip marshal then unmarshal preserves AppRec fields`() {
        val fellesformat = unmarshal(loadResource("trekkopplysning_avvisning.xml"), EIFellesformat::class.java)
        val appRec = fellesformat.appRec

        val xml = String(apprecTrekkopplysningMarshaller.marshalToByteArray(appRec))
        val restored = apprecTrekkopplysningMarshaller.unmarshal(xml, AppRec::class.java)

        assertEquals("f2f0f2f6-f0f5-f0f4-f1f4-f2f6f0f3f4f8", restored.id)
        assertEquals("2", restored.status.v)
        assertEquals("Avvist", restored.status.dn)
    }
}
