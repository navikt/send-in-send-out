package no.nav.emottak.util

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.serialization.Serializable
import no.nav.emottak.fellesformat.FellesFormatXmlMarshaller
import no.nav.emottak.fellesformat.asEIFellesFormat
import no.nav.emottak.validSendInPasientlisteRequest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.Marker
import javax.xml.bind.JAXBContext
import javax.xml.bind.annotation.XmlRootElement

@XmlRootElement // for JAXB
@Serializable // for Kotlinx
data class Person(
    var name: String = "",
    var age: Int = 0
)

class AsXLoggerTest {

    @Test
    fun `test asJson`() {
        val mockLogger = mockk<Logger>(relaxed = true)
        val capturedJson = slot<String>()
        every { mockLogger.debug(any<Marker>(), "{}: {}", "Test JSON", capture(capturedJson)) } returns Unit

        val person = Person("Alice", 30)
        mockLogger.asJson(LogLevel.DEBUG, "Test JSON", person, Person.serializer())

        verify(exactly = 1) { mockLogger.debug(any<Marker>(), "{}: {}", "Test JSON", any()) }
        assertTrue(capturedJson.captured.contains("\"name\":\"Alice\"")) {
            "Generated JSON should contain name field, but was: ${capturedJson.captured}"
        }
        assertTrue(capturedJson.captured.contains("\"age\":30")) {
            "Generated JSON should contain age field, but was: ${capturedJson.captured}"
        }
    }

    @Test
    fun `test asXml with test marshaller`() {
        val mockLogger = mockk<Logger>(relaxed = true)
        val capturedXml = slot<String>()
        every { mockLogger.debug(any<Marker>(), "{}: {}", "Test XML", capture(capturedXml)) } returns Unit

        val testMarshaller = XmlMarshaller(
            JAXBContext.newInstance(Person::class.java)
        )

        val person = Person("Bob", 25)
        mockLogger.asXml(LogLevel.DEBUG, "Test XML", person, testMarshaller)

        verify(exactly = 1) { mockLogger.debug(any<Marker>(), "{}: {}", "Test XML", any()) }
        assertTrue(capturedXml.captured.contains("<person>")) {
            "Generated XML should contain person element, but was: ${capturedXml.captured}"
        }
        assertTrue(capturedXml.captured.contains("<name>Bob</name>")) {
            "Generated XML should contain name element, but was: ${capturedXml.captured}"
        }
        assertTrue(capturedXml.captured.contains("<age>25</age>")) {
            "Generated XML should contain age element, but was: ${capturedXml.captured}"
        }
    }

    @Test
    fun `test asXml with FellesFormat marshaller`() {
        val mockLogger = mockk<Logger>(relaxed = true)
        val capturedXml = slot<String>()
        every { mockLogger.debug(any<Marker>(), "{}: {}", "FellesFormat XML", capture(capturedXml)) } returns Unit

        val fellesformat = validSendInPasientlisteRequest.value.asEIFellesFormat()
        mockLogger.asXml(
            LogLevel.DEBUG,
            "FellesFormat XML",
            fellesformat,
            FellesFormatXmlMarshaller
        )

        verify(exactly = 1) { mockLogger.debug(any<Marker>(), "{}: {}", "FellesFormat XML", any()) }
        assertTrue(capturedXml.captured.contains("EI_fellesformat")) {
            "Generated XML should contain EI_fellesformat element, but was: ${capturedXml.captured}"
        }
    }
}
