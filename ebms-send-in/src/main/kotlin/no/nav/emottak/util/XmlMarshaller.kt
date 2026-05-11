package no.nav.emottak.util

import com.sun.xml.bind.marshaller.NamespacePrefixMapper
import org.w3c.dom.Node
import java.io.ByteArrayOutputStream
import java.io.StringWriter
import javax.xml.bind.JAXBContext
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamWriter

class XmlMarshaller(jaxbContext: JAXBContext, namespacePrefixMapper: NamespacePrefixMapper? = null) {
    private val marshaller = jaxbContext.createMarshaller().apply {
        if (namespacePrefixMapper != null) {
            this.setProperty("com.sun.xml.bind.namespacePrefixMapper", namespacePrefixMapper)
        }
    }
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

    fun marshal(objekt: Any, xmlStreamWriter: XMLStreamWriter) {
        synchronized(marshallingMonitor) {
            marshaller.marshal(objekt, xmlStreamWriter)
        }
    }

    fun toDomainObject(any: Any): Any {
        return synchronized(unmarshallingMonitor) {
            unmarshaller.unmarshal(any as Node)
        }
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
