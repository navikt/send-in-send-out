package no.nav.emottak.util

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.Marker
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
        assertTrue(capturedJson.captured.contains("\"name\":\"Alice\""))
        assertTrue(capturedJson.captured.contains("\"age\":30"))
    }

    @Test
    fun `test asXml`() {
        val mockLogger = mockk<Logger>(relaxed = true)
        val capturedXml = slot<String>()
        every { mockLogger.debug(any<Marker>(), "{}: {}", "Test XML", capture(capturedXml)) } returns Unit

        val person = Person("Bob", 25)
        mockLogger.asXml(LogLevel.DEBUG, "Test XML", person)

        verify(exactly = 1) { mockLogger.debug(any<Marker>(), "{}: {}", "Test XML", any()) }
        println(capturedXml.captured)
        assertTrue(capturedXml.captured.contains("<person>"))
        assertTrue(capturedXml.captured.contains("<name>Bob</name>"))
        assertTrue(capturedXml.captured.contains("<age>25</age>"))
    }
}
