package no.nav.emottak.legemelding

import no.nav.emottak.util.XmlMarshaller
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import no.trygdeetaten.xml.eiff._1.ObjectFactory
import org.apache.cxf.common.jaxb.NamespaceMapper
import java.io.StringWriter
import javax.xml.bind.JAXBContext.newInstance
import javax.xml.stream.XMLOutputFactory

val legemeldingXmlMarshaller = XmlMarshaller(

    newInstance(
        ObjectFactory::class.java
    ),
    NamespaceMapper(
        mapOf(
            "" to "http://www.kith.no/xmlstds/msghead/2006-05-24",
            "ns2" to "http://www.w3.org/2000/09/xmldsig#",
            "ns3" to "http://www.trygdeetaten.no/xml/eiff/1/",
            "ns4" to "http://www.kith.no/xmlstds/legeerklaring/2008-06-06"
        )
    )
)

fun marshalLegemelding(fellesFormat: EIFellesformat): String {
    val writer = StringWriter()
    val xmlStreamWriter = XMLOutputFactory.newFactory().createXMLStreamWriter(writer)
    legemeldingXmlMarshaller.marshal(fellesFormat, xmlStreamWriter)
    return writer.toString()
}
