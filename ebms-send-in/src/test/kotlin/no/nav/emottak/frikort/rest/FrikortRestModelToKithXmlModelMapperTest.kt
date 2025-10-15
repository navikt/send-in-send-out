package no.nav.emottak.frikort.rest

import kotlinx.datetime.Clock
import no.helsedir.frikort.frikorttjenester.model.CS
import no.helsedir.frikort.frikorttjenester.model.Content
import no.helsedir.frikort.frikorttjenester.model.Document
import no.helsedir.frikort.frikorttjenester.model.EgenandelSvar
import no.helsedir.frikort.frikorttjenester.model.MsgHead
import no.helsedir.frikort.frikorttjenester.model.MsgInfo
import no.helsedir.frikort.frikorttjenester.model.Organization
import no.helsedir.frikort.frikorttjenester.model.Receiver
import no.helsedir.frikort.frikorttjenester.model.Sender
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FrikortRestModelToKithXmlModelMapperTest {

    @Test
    fun `Map from Frikort model to MsgHead`() {
        val msgHead = MsgHead(
            msgInfo = MsgInfo(
                type = CS(v = "Egenandelsvar", dn = "Type"),
                migVersion = "v1.2 2006-05-24",
                genDate = Clock.System.now(),
                msgId = "id",
                sender = Sender(Organization("SenderOrg")),
                receiver = Receiver(Organization("ReceiverOrg"))
            ),
            documents = listOf(
                Document(
                    refDoc = no.helsedir.frikort.frikorttjenester.model.RefDoc(
                        msgType = CS(v = "XML", dn = "XML-instans"),
                        mimeType = "application/xml",
                        content = Content(
                            egenandelSvar = EgenandelSvar(
                                status = CS(v = "0", dn = "Ingen data"),
                                svarMelding = "Informasjon om fritak fra egenandel er ikke tilgjengelig."
                            )
                        )
                    )
                )
            )
        )
        val result = msgHead.toMsgHead()
        val any = result.document[0].refDoc.content.any[0]
        assertTrue(any is no.kith.xmlstds.nav.egenandel._2010_02_01.EgenandelSvar)
        val svar = any as no.kith.xmlstds.nav.egenandel._2010_02_01.EgenandelSvar
        assertEquals("0", svar.status.v)
        assertEquals("Ingen data", svar.status.dn)
        assertEquals("Informasjon om fritak fra egenandel er ikke tilgjengelig.", svar.svarmelding)
    }

    @Test
    fun `Unknown content in document list throws exception`() {
        val content = Content()
        val document = Document(
            refDoc = no.helsedir.frikort.frikorttjenester.model.RefDoc(
                msgType = CS(v = "type", dn = "Type"),
                mimeType = "application/xml",
                content = content
            )
        )
        val msgHead = MsgHead(
            msgInfo = MsgInfo(
                type = CS(v = "type", dn = "Type"),
                migVersion = "1.0",
                genDate = Clock.System.now(),
                msgId = "id",
                sender = Sender(Organization("SenderOrg")),
                receiver = Receiver(Organization("ReceiverOrg"))
            ),
            documents = listOf(document)
        )
        val exception = assertThrows(RuntimeException::class.java) {
            msgHead.toMsgHead()
        }
        assertTrue(exception.message!!.contains("Unknown return content type"))
    }
}
