package no.nav.emottak.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.Marker
import java.io.StringWriter
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException
import javax.xml.bind.Marshaller

fun <T> Logger.asJson(
    logLevel: LogLevel = LogLevel.DEBUG,
    message: String,
    obj: T,
    serializer: KSerializer<T>,
    marker: Marker? = null
) {
    try {
        val json = Json.encodeToString(serializer, obj)
        when (logLevel) {
            LogLevel.INFO -> this.info(marker, "{}: {}", message, json)
            LogLevel.DEBUG -> this.debug(marker, "{}: {}", message, json)
            LogLevel.WARN -> this.warn(marker, "{}: {}", message, json)
            LogLevel.ERROR -> this.error(marker, "{}: {}", message, json)
        }
    } catch (e: Exception) {
        this.error("Failed to serialize object for logging: {}", e.message, e)
    }
}

fun <T> Logger.asXml(
    logLevel: LogLevel = LogLevel.DEBUG,
    message: String,
    obj: T,
    marker: Marker? = null
) {
    try {
        val jaxbContext = JAXBContext.newInstance(obj!!::class.java)
        val marshaller: Marshaller = jaxbContext.createMarshaller()
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)

        val writer = StringWriter()
        marshaller.marshal(obj, writer)
        val xmlContent = writer.toString()

        when (logLevel) {
            LogLevel.INFO -> this.info(marker, "{}: {}", message, xmlContent)
            LogLevel.DEBUG -> this.debug(marker, "{}: {}", message, xmlContent)
            LogLevel.WARN -> this.warn(marker, "{}: {}", message, xmlContent)
            LogLevel.ERROR -> this.error(marker, "{}: {}", message, xmlContent)
        }
    } catch (e: JAXBException) {
        this.error("Failed to serialize object to XML: {}", e.message, e)
    }
}

enum class LogLevel {
    INFO, DEBUG, WARN, ERROR
}
