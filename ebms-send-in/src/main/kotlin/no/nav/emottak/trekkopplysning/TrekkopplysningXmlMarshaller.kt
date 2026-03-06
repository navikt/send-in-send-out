package no.nav.emottak.trekkopplysning

import no.nav.emottak.util.XmlMarshaller
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import no.trygdeetaten.xml.eiff._1.ObjectFactory
import org.apache.cxf.staxutils.DelegatingXMLStreamWriter
import java.io.StringWriter
import javax.xml.bind.JAXBContext.newInstance
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter

val trekkopplysningXmlMarshaller = XmlMarshaller(
    newInstance(
        //            org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ObjectFactory::class.java,
        //            org.xmlsoap.schemas.soap.envelope.ObjectFactory::class.java,
        //            org.w3._1999.xlink.ObjectFactory::class.java,
        //            org.w3._2009.xmldsig11_.ObjectFactory::class.java,
        //            no.kith.xmlstds.msghead._2006_05_24.ObjectFactory::class.java,
        ObjectFactory::class.java
    )
)

fun marshalTrekkopplysning(fellesFormat: EIFellesformat): String {
    val writer = StringWriter()
    val xmlStreamWriter = TrekkopplysningWriter(XMLOutputFactory.newFactory().createXMLStreamWriter(writer))
    trekkopplysningXmlMarshaller.marshal(fellesFormat, xmlStreamWriter)
    return writer.toString()
}

// Denne XML-writeren overstyrer normal serialisering for å få XML a la gamle eMottak:
// Det brukes IKKE namespace-prefikser, hvert namespace deklareres som default NS inni topp-elementet det hører til
class TrekkopplysningWriter(writer: XMLStreamWriter) : DelegatingXMLStreamWriter(writer) {

    override fun writeStartElement(namespaceURI: String?, localName: String?, prefix: String?) {
        if (localName == "EI_fellesformat") {
            super.writeStartElement("", "EI_fellesformat", "")
            super.writeDefaultNamespace("http://www.trygdeetaten.no/xml/eiff/1/")
        } else if (localName == "MsgHead") {
            super.writeStartElement("", "MsgHead", "")
            super.writeDefaultNamespace("http://www.kith.no/xmlstds/msghead/2006-05-24")
        } else if (localName == "Signature") {
            super.writeStartElement("", "Signature", "")
            super.writeDefaultNamespace("http://www.w3.org/2000/09/xmldsig#")
        } else {
            super.writeStartElement("", localName, "")
        }
    }

    override fun writeNamespace(prefix: String?, uri: String?) {
        // Ønsker ikke andre deklarasjoner enn de som eksplisitt er gjort i writeStartElement
        // super.writeNamespace(prefix, uri)
    }
}
