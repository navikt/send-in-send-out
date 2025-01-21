package no.nav.emottak.util

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.Logger
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
        every { mockLogger.info("{}: {}", "Test JSON", capture(capturedJson)) } returns Unit

        val person = Person("Alice", 30)
        mockLogger.asJson(LogLevel.INFO, "Test JSON", person, Person.serializer())

        verify(exactly = 1) { mockLogger.info("{}: {}", "Test JSON", any()) }
        assertTrue(capturedJson.captured.contains("\"name\":\"Alice\""))
        assertTrue(capturedJson.captured.contains("\"age\":30"))
    }

    @Test
    fun `test asXml`() {
        val mockLogger = mockk<Logger>(relaxed = true)
        val capturedXml = slot<String>()
        every { mockLogger.info("{}: {}", "Test XML", capture(capturedXml)) } returns Unit

        val person = Person("Bob", 25)
        mockLogger.asXml(LogLevel.INFO, "Test XML", person)

        verify(exactly = 1) { mockLogger.info("{}: {}", "Test XML", any()) }
        println(capturedXml.captured)
        assertTrue(capturedXml.captured.contains("<person>"))
        assertTrue(capturedXml.captured.contains("<name>Bob</name>"))
        assertTrue(capturedXml.captured.contains("<age>25</age>"))
    }
}
