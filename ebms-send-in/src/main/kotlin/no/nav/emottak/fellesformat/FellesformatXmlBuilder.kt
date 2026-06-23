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

    // Bygger XML uten unmarshal/marshal, for å beholde input-payload uendret.
    // Produsert XML vil ha samme namespace-definisjoner som input, men vil få sorterte attributter.
    // Siden det finnes meldingstyper som må ha annen sortering av attributter i MottakenhetBlokk,
    // finnes det en variant som hardkoder MottakenhetBlokk-XML med skreddersydd attributt-sortering.
    // Det er mulig å bygge XML enda mer tekst-basert (uten å la payload gå via DOM),
    // men man må da ta høyde for at payload kan inneholde XML-prolog.

    // TODO kan slette marshaller for trekkopplysning og sykmelding, de brukes ikke
    // Venter til vi har verifisert over tid at builder-måten virker.
    // Det er lite testdata, og vi har hatt en stygg feil i prod ved å endre trekkopplysning-generering

    fun buildXml(mottakenhetBlokk: EIFellesformat.MottakenhetBlokk, payload: ByteArray): String {
        val doc = buildFellesformatDocument(mottakenhetBlokk, payload)
        return toXml(doc)
    }

    fun buildXmlWithCustomMottakenhetBlokk(mottakenhetBlokk: EIFellesformat.MottakenhetBlokk, payload: ByteArray): String {
        val doc = buildFellesformatDocumentWithoutMottakenhetBlokk(payload)
        return toXmlAddingMottakenhetBlokk(doc, mottakenhetBlokk)
    }

    // Skreddersydd attributt-sortering, verifisert for trekkopplysning og sykmelding
    fun buildCustomXml(m: EIFellesformat.MottakenhetBlokk): String {
        var xml = "<MottakenhetBlokk"
        if (m.ediLoggId != null) xml += " ediLoggId=\"${m.ediLoggId}\""
        if (m.avsender != null) xml += " avsender=\"${m.avsender}\""
        if (m.ebXMLSamtaleId != null) xml += " ebXMLSamtaleId=\"${m.ebXMLSamtaleId}\""
        if (m.meldingsType != null) xml += " meldingsType=\"${m.meldingsType}\""
        if (m.avsenderRef != null) xml += " avsenderRef=\"${m.avsenderRef}\""
        if (m.avsenderFnrFraDigSignatur != null) xml += " avsenderFnrFraDigSignatur=\"${m.avsenderFnrFraDigSignatur}\""
        if (m.mottattDatotid != null) xml += " mottattDatotid=\"${m.mottattDatotid.toXMLFormat()}\""
        if (m.orgNummer != null) xml += " orgNummer=\"${m.orgNummer}\""
        if (m.partnerReferanse != null) xml += " partnerReferanse=\"${m.partnerReferanse}\""
        if (m.herIdentifikator != null) xml += " herIdentifikator=\"${m.herIdentifikator}\""
        if (m.ebAction != null) xml += " ebAction=\"${m.ebAction}\""
        if (m.ebRole != null) xml += " ebRole=\"${m.ebRole}\""
        if (m.ebService != null) xml += " ebService=\"${m.ebService}\""
        xml += "/>"
        return xml
    }

    fun buildFellesformatDocument(mottakenhetBlokk: EIFellesformat.MottakenhetBlokk?, payload: ByteArray): Document {
        val f: DocumentBuilderFactory = createDocumentBuilderFactory()
        val doc = f.newDocumentBuilder().newDocument()
        doc.xmlStandalone = true
        val ffElement = doc.createElementNS("http://www.trygdeetaten.no/xml/eiff/1/", "EI_fellesformat")
        doc.appendChild(ffElement)

        val payloadDoc = f.newDocumentBuilder().parse(ByteArrayInputStream(payload))
        val payloadElement = payloadDoc.childNodes.item(0) // MsgHead
        val msgHead = doc.importNode(payloadElement, true)
        ffElement.appendChild(msgHead)

        if (mottakenhetBlokk != null) {
            ffElement.appendChild(buildMottakenhetBlokkElement(doc, mottakenhetBlokk))
        }

        return doc
    }

    fun buildFellesformatDocumentWithoutMottakenhetBlokk(payload: ByteArray): Document {
        return buildFellesformatDocument(null, payload)
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
        if (m.avsenderFnrFraDigSignatur != null) e.setAttribute("avsenderFnrFraDigSignatur", m.avsenderFnrFraDigSignatur)
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

    fun toXmlAddingMottakenhetBlokk(doc: Document, mottakenhetBlokk: EIFellesformat.MottakenhetBlokk): String {
        val result = StringWriter()
        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.transform(DOMSource(doc), StreamResult(result))
        val docXml = result.toString()
        val mottakenhetBlokkXml = buildCustomXml(mottakenhetBlokk)

        val tokenWithoutNamespace = "</EI_fellesformat>"
        val insertPos = docXml.indexOf(tokenWithoutNamespace)
        if (insertPos != -1) return docXml.substring(0, insertPos) + mottakenhetBlokkXml + docXml.substring(insertPos)

        val tokenWithNamespace = Regex("</ns\\d*:EI_fellesformat>")
        val found = tokenWithNamespace.find(docXml)
        if (found != null) {
            val insertPos = found.range.first
            return docXml.substring(0, insertPos) + mottakenhetBlokkXml + docXml.substring(insertPos)
        }

        return docXml
    }

    private fun createDocumentBuilderFactory(): DocumentBuilderFactory =
        DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            isXIncludeAware = false
            isExpandEntityReferences = false
            isNamespaceAware = true
        }
}
