package no.nav.emottak.util

import java.io.StringWriter
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException
import javax.xml.bind.Marshaller
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.slf4j.Logger


fun <T> Logger.asJson(
    logLevel: LogLevel = LogLevel.INFO,
    message: String,
    obj: T,
    serializer: KSerializer<T>
) {
    try {
        val json = Json.encodeToString(serializer, obj)
        when (logLevel) {
            LogLevel.INFO -> this.info("{}: {}", message, json)
            LogLevel.DEBUG -> this.debug("{}: {}", message, json)
            LogLevel.WARN -> this.warn("{}: {}", message, json)
            LogLevel.ERROR -> this.error("{}: {}", message, json)
        }
    } catch (e: Exception) {
        this.error("Failed to serialize object for logging: {}", e.message, e)
    }
}


fun <T> Logger.asXml(
    logLevel: LogLevel = LogLevel.INFO,
    message: String,
    obj: T
) {
    try {
        val jaxbContext = JAXBContext.newInstance(obj!!::class.java)
        val marshaller: Marshaller = jaxbContext.createMarshaller()
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)

        val writer = StringWriter()
        marshaller.marshal(obj, writer)
        val xmlContent = writer.toString()

        when (logLevel) {
            LogLevel.INFO -> this.info("{}: {}", message, xmlContent)
            LogLevel.DEBUG -> this.debug("{}: {}", message, xmlContent)
            LogLevel.WARN -> this.warn("{}: {}", message, xmlContent)
            LogLevel.ERROR -> this.error("{}: {}", message, xmlContent)
        }
    } catch (e: JAXBException) {
        this.error("Failed to serialize object to XML: {}", e.message, e)
    }
}



enum class LogLevel {
    INFO, DEBUG, WARN, ERROR
}