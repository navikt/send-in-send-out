package no.nav.emottak.ebms

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.emottak.ebms.ebxml.errorList
import no.nav.emottak.ebms.ebxml.messageHeader
import no.nav.emottak.ebms.model.EbMSErrorUtil
import no.nav.emottak.ebms.processing.ProcessingService
import no.nav.emottak.ebms.validation.DokumentValidator
import no.nav.emottak.ebms.validation.MimeHeaders
import no.nav.emottak.ebms.xml.xmlMarshaller
import no.nav.emottak.melding.model.ValidationResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.ErrorList
import org.xmlsoap.schemas.soap.envelope.Envelope
import org.xmlsoap.schemas.soap.envelope.Fault
import javax.xml.bind.JAXBElement


class EbmsRouteTest {
    val validMultipartRequest = validMultipartRequest()
    val cpaRepoClient = mockk<CpaRepoClient>()


    fun <T> mimeTestApp(testBlock: suspend ApplicationTestBuilder.() -> T) = testApplication {

        application {
            val dokumentValidator = DokumentValidator(cpaRepoClient)
            val processingService = mockk<ProcessingService>()
            routing {
                postEbms(dokumentValidator,processingService,cpaRepoClient)
            }

        }
        testBlock()
    }




    @Test
    fun `Soap Fault om Mime Feil`() = mimeTestApp {

        val wrongMime = validMultipartRequest.modify {
            it.remove(MimeHeaders.MIME_VERSION)
        }

        var response = client.post("/ebms",wrongMime.asHttpRequest())
        var envelope:Envelope =  xmlMarshaller.unmarshal(response.bodyAsText(),Envelope::class.java)
        with (envelope.assertFaultAndGet()) {
            assertEquals("MIME version is missing or incorrect", this.faultstring)
            assertEquals("Server", this.faultcode.localPart)
        }

        val wrongHeader = validMultipartRequest.modify(validMultipartRequest.parts.first() to validMultipartRequest.parts.first().modify {
            it.remove(MimeHeaders.CONTENT_TRANSFER_ENCODING)
        })
        response = client.post("/ebms", wrongHeader.asHttpRequest())
        envelope = xmlMarshaller.unmarshal(response.bodyAsText(),Envelope::class.java)
        with (envelope.assertFaultAndGet()) {
             assertEquals("Mandatory header Content-Transfer-Encoding is undefined", this.faultstring)
             assertEquals("Server", this.faultcode.localPart)
        }
        println(envelope)


    }

    @Test
    fun `Sending unparsable xml as dokument should Soap Fault`()  = mimeTestApp {

         val illegalContent = validMultipartRequest.modify(validMultipartRequest.parts.first() to validMultipartRequest.parts.first().payload("Illegal payload"))

                val response = client.post("/ebms",illegalContent.asHttpRequest())
                val envelope =  xmlMarshaller.unmarshal(response.bodyAsText(),Envelope::class.java)
                with(envelope.assertFaultAndGet()) {
                    assertEquals("Unable to transform request into EbmsDokument: Invalid byte 1 of 1-byte UTF-8 sequence.", this.faultstring)
                    assertEquals("Server", this.faultcode.localPart)
                }

    }

    @Test
    fun `Sending valid request should trigger validation`() = mimeTestApp {
        coEvery {
            cpaRepoClient.postValidate(any())
        } returns ValidationResult(valid = false)

        val response = client.post("/ebms",validMultipartRequest.asHttpRequest())
        val envelope =  xmlMarshaller.unmarshal(response.bodyAsText(),Envelope::class.java)
        with(envelope.assertErrorAndGet().error.first()) {
            assertEquals("Validation failed" , this.description.value)
            assertEquals(EbMSErrorUtil.Code.OTHER_XML.name,this.errorCode)
        }

    }

    fun Envelope.assertErrorAndGet(): ErrorList {
        assertNotNull(this.header.messageHeader())
        assertNotNull(this.header.errorList())
        return this.header.errorList()!!
    }

    fun Envelope.assertFaultAndGet(): Fault =
        this.body.any.first()
            .let {
                assertTrue(it is JAXBElement<*>)
                it as JAXBElement<*>
            }.let {
                assertTrue( it.value is Fault)
                it.value as Fault
            }


}



