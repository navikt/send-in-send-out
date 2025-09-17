package no.nav.emottak.util

import org.w3c.dom.Node
import java.io.ByteArrayOutputStream
import java.io.StringWriter
import javax.xml.bind.JAXBContext
import javax.xml.stream.XMLInputFactory

class XmlMarshaller(jaxbContext: JAXBContext) {
    private val marshaller = jaxbContext.createMarshaller()
    private val unmarshaller = jaxbContext.createUnmarshaller()
    private val marshallingMonitor = Any()
    private val unmarshallingMonitor = Any()

    fun marshal(objekt: Any): String {
        val writer = StringWriter()
        synchronized(marshallingMonitor) {
            marshaller.marshal(objekt, writer)
        }
        return writer.toString()
    }

    fun toDomainObject(any: Any): Any {
        return unmarshaller.unmarshal(any as Node)
    }

    fun marshalToByteArray(objekt: Any): ByteArray {
        return ByteArrayOutputStream().use {
            synchronized(marshallingMonitor) {
                marshaller.marshal(objekt, it)
            }
            it.toByteArray()
        }
    }

    fun <T> unmarshal(xml: String, clazz: Class<T>): T {
        val reader = XMLInputFactory.newInstance().createXMLStreamReader(xml.reader())
        return synchronized(unmarshallingMonitor) {
            unmarshaller.unmarshal(reader, clazz).value
        }
    }
}
