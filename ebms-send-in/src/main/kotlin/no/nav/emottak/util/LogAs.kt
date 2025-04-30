package no.nav.emottak.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.Marker

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
            LogLevel.INFO -> info(marker, "{}: {}", message, json)
            LogLevel.DEBUG -> debug(marker, "{}: {}", message, json)
            LogLevel.WARN -> warn(marker, "{}: {}", message, json)
            LogLevel.ERROR -> error(marker, "{}: {}", message, json)
        }
    } catch (e: Exception) {
        error("Failed to serialize object for logging: {}", e.message, e)
    }
}

fun Logger.asXml(
    logLevel: LogLevel = LogLevel.DEBUG,
    message: String,
    obj: Any,
    marshaller: XmlMarshaller,
    marker: Marker? = null
) {
    try {
        val xmlContent = marshaller.marshal(obj)
        when (logLevel) {
            LogLevel.INFO -> info(marker, "{}: {}", message, xmlContent)
            LogLevel.DEBUG -> debug(marker, "{}: {}", message, xmlContent)
            LogLevel.WARN -> warn(marker, "{}: {}", message, xmlContent)
            LogLevel.ERROR -> error(marker, "{}: {}", message, xmlContent)
        }
    } catch (e: Exception) {
        error("Failed to serialize object to XML: {}", e.message, e)
    }
}

enum class LogLevel {
    INFO, DEBUG, WARN, ERROR
}
