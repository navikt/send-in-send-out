package no.nav.emottak.fellesformat

import no.trygdeetaten.xml.eiff._1.EIFellesformat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class FellesformatXmlBuilderTest {

    private val builder = FellesformatXmlBuilder()

    private val mottakenhetBlokk = EIFellesformat.MottakenhetBlokk().apply {
        ediLoggId = "1"
        ebService = "service"
    }

    private val payloadWithoutComment =
        """<MsgHead xmlns="http://www.kith.no/xmlstds/msghead/2006-05-24"><MsgInfo/></MsgHead>"""

    // Regression case: a comment before the root element is legal XML and used to make
    // payloadDoc.childNodes.item(0) return the Comment node instead of the MsgHead element.
    private val payloadWithLeadingComment =
        """<?xml version="1.0" encoding="UTF-8"?>
            |<!-- some comment -->
            |<MsgHead xmlns="http://www.kith.no/xmlstds/msghead/2006-05-24"><MsgInfo/></MsgHead>
        """.trimMargin()

    @Test
    fun `buildFellesformatDocument extracts MsgHead as root child when payload has no leading comment`() {
        val doc = builder.buildFellesformatDocument(mottakenhetBlokk, payloadWithoutComment.toByteArray(StandardCharsets.UTF_8))

        val msgHead = doc.documentElement.firstChild
        assertNotNull(msgHead)
        assertEquals("MsgHead", msgHead.localName)
    }

    @Test
    fun `buildFellesformatDocument extracts MsgHead as root child when payload has a leading comment`() {
        val doc = builder.buildFellesformatDocument(mottakenhetBlokk, payloadWithLeadingComment.toByteArray(StandardCharsets.UTF_8))

        val msgHead = doc.documentElement.firstChild
        assertNotNull(msgHead)
        assertEquals("MsgHead", msgHead.localName)
    }

    @Test
    fun `buildXmlWithCustomMottakenhetBlokk produces MsgHead element when payload has a leading comment`() {
        val xml = builder.buildXmlWithCustomMottakenhetBlokk(mottakenhetBlokk, payloadWithLeadingComment.toByteArray(StandardCharsets.UTF_8))

        assertEquals(true, xml.contains("<MsgHead"))
        assertEquals(false, xml.contains("<!--"))
    }
}
