package no.nav.emottak.fellesformat

import no.trygdeetaten.xml.eiff._1.EIFellesformat
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class FellesformatXmlBuilder {

    // Build XML WITHOUT marshalling, to keep the input payload exactly as it is
    // This will keep namespaces as they are, but attributes will be re-sorted alphabetically

    fun buildFellesformatDocument(mottakenhetBlokk: EIFellesformat.MottakenhetBlokk, payload: ByteArray): Document {
        val f: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()
        val doc = f.newDocumentBuilder().newDocument()
        doc.xmlStandalone = true
        val ffElement = doc.createElementNS("http://www.trygdeetaten.no/xml/eiff/1/", "EI_fellesformat")
        doc.appendChild(ffElement)

        val payloadDoc = f.newDocumentBuilder().parse(ByteArrayInputStream(payload))
        val payloadElement = payloadDoc.childNodes.item(0) // MsgHead
        val msgHead = doc.importNode(payloadElement, true)
        ffElement.appendChild(msgHead)

        ffElement.appendChild(buildMottakenhetBlokkElement(doc, mottakenhetBlokk))

        return doc
    }

    fun buildMottakenhetBlokkElement(doc: Document, m: EIFellesformat.MottakenhetBlokk): Element {
        val e = doc.createElement("MottakenhetBlokk")
        if (m.ediLoggId != null) e.setAttribute("ediLoggId", m.ediLoggId)
        if (m.ebXMLSamtaleId != null) e.setAttribute("ebXMLSamtaleId", m.ebXMLSamtaleId)
        if (m.ebAction != null) e.setAttribute("ebAction", m.ebAction)
        if (m.ebRole != null) e.setAttribute("ebRole", m.ebRole)
        if (m.ebService != null) e.setAttribute("ebService", m.ebService)
        if (m.herIdentifikator != null) e.setAttribute("herIdentifikator", m.herIdentifikator)
        if (m.orgNummer != null) e.setAttribute("orgNummer", m.orgNummer)
        if (m.partnerReferanse != null) e.setAttribute("partnerReferanse", m.partnerReferanse)
        if (m.avsender != null) e.setAttribute("avsender", m.avsender)
        if (m.avsenderRef != null) e.setAttribute("avsenderRef", m.avsenderRef)
        if (m.meldingsType != null) e.setAttribute("meldingsType", m.meldingsType)
        if (m.mottattDatotid != null) e.setAttribute("mottattDatotid", m.mottattDatotid.toXMLFormat())
        return e
    }

    fun toXml(doc: Document): String {
        val result = StringWriter()
        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.transform(DOMSource(doc), StreamResult(result))
        return result.toString()
    }
}
