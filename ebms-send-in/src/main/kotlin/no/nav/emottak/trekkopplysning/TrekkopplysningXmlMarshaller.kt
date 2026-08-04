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
        ObjectFactory::class.java
    )
)

val msgheadTrekkopplysningMarshaller = XmlMarshaller(
    newInstance(
        no.kith.xmlstds.msghead._2006_05_24.ObjectFactory::class.java,
        org.w3._1999.xlink.ObjectFactory::class.java,
        org.w3._2009.xmldsig11_.ObjectFactory::class.java
    )
)

val apprecTrekkopplysningMarshaller = XmlMarshaller(
    newInstance(
        no.kith.xmlstds.apprec._2004_11_21.ObjectFactory::class.java,
        org.w3._1999.xlink.ObjectFactory::class.java,
        org.w3._2009.xmldsig11_.ObjectFactory::class.java
    ),
    schemaLocation = "http://www.kith.no/xmlstds/apprec/2004-11-21 AppRec-v1-2004-11-21.xsd"
)

fun marshalTrekkopplysning(fellesFormat: EIFellesformat): String {
    val writer = StringWriter()
    val xmlStreamWriter = TrekkopplysningWriter(XMLOutputFactory.newFactory().createXMLStreamWriter(writer))
    trekkopplysningXmlMarshaller.marshal(fellesFormat, xmlStreamWriter)
    return writer.toString()
}

// Denne XML-writeren overstyrer normal serialisering for å få XML a la gamle eMottak:
// Det brukes IKKE namespace-prefikser, hvert namespace deklareres som default NS inni topp-elementet det hører til
// Virker som mottakerne må ha det EKSAKT som kodet under
// I tillegg SKAL service-action-role attributtene i MottakenhetBlokk komme i helt spesifikk rekkefølge.
class TrekkopplysningWriter(writer: XMLStreamWriter) : DelegatingXMLStreamWriter(writer) {

    // I element hvor attributtene skal komme spesialsortert, cacher vi dem til slutt-tagen skal skrives
    var deferAttributeWritingToElementEnd: Boolean = false
    val cachedAttributesWithValues: MutableMap<String, String> = mutableMapOf()

    // Attributtene som skal sorteres, i ønsket rekkefølge
    val attributesToSort = listOf("ebAction", "ebRole", "ebService")

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
        } else if (localName == "InnrapporteringTrekk") {
            super.writeStartElement("", "InnrapporteringTrekk", "")
            super.writeDefaultNamespace("http://www.kith.no/xmlstds/nav/innrapporteringtrekk/2010-02-04")
            super.writeAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
            // Ser ut til at den under kommer uansett
//            super.writeAttribute("xsi:schemaLocation", "http://www.kith.no/xmlstds/nav/innrapporteringtrekk/2010-02-04 InnrapporteringTrekk-2010-02-04.xsd")
        } else {
            super.writeStartElement("", localName, "")
        }

        if (localName == "MottakenhetBlokk") {
            deferAttributeWritingToElementEnd = true
        } else {
            deferAttributeWritingToElementEnd = false
        }
    }

    override fun writeNamespace(prefix: String?, uri: String?) {
        // Ønsker ikke andre deklarasjoner enn de som eksplisitt er gjort i writeStartElement
        // super.writeNamespace(prefix, uri)
    }

    override fun writeAttribute(local: String, value: String) {
        if (deferAttributeWritingToElementEnd && local in attributesToSort) {
            cachedAttributesWithValues.put(local, value)
        } else {
            super.writeAttribute(local, value)
        }
    }

    override fun writeEndElement() {
        if (!cachedAttributesWithValues.isEmpty()) {
            for (attributeName in attributesToSort) {
                writeAttributeIfValueIsCached(attributeName)
            }
            cachedAttributesWithValues.clear()
        }
        super.writeEndElement()
    }

    private fun writeAttributeIfValueIsCached(attributeName: String) {
        if (cachedAttributesWithValues[attributeName] != null) {
            super.writeAttribute(attributeName, cachedAttributesWithValues[attributeName]!!)
        }
    }
}
