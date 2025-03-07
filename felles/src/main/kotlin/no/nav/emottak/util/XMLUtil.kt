package no.nav.emottak.util

import no.kith.xmlstds.nav.egenandel._2010_02_01.EgenandelForesporsel
import no.kith.xmlstds.nav.egenandel._2016_06_10.EgenandelForesporselV2
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.Instant
import java.util.GregorianCalendar
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

const val BIRTHDAY: Int = 6
const val FNUMBER: Int = 11

fun refParam(fellesformat: EIFellesformat): String {
    val egenandelforesporsel = fellesformat.msgHead.document.first().refDoc.content.any.first()
    return when (egenandelforesporsel) {
        is EgenandelForesporselV2 -> {
            egenandelforesporsel.harBorgerEgenandelfritak?.borgerFnr ?: egenandelforesporsel.harBorgerFrikort?.borgerFnr ?: "NA"
        }
        is EgenandelForesporsel -> {
            egenandelforesporsel.harBorgerEgenandelfritak?.borgerFnr ?: egenandelforesporsel.harBorgerFrikort?.borgerFnr ?: "NA"
        }
        else -> "NA"
    }
}

fun birthDay(fnr: String): String {
    if (fnr.length != FNUMBER) {
        return "NA"
    }
    return fnr.substring(0, BIRTHDAY)
}

fun createDocument(inputstream: InputStream): Document {
    val dbf = DocumentBuilderFactory.newInstance()
    dbf.isNamespaceAware = true
    return dbf.newDocumentBuilder().parse(inputstream)
}

fun getByteArrayFromDocument(doc: Document): ByteArray {
    val outputStream = ByteArrayOutputStream()
    val xmlSource = DOMSource(doc)
    val result = StreamResult(outputStream)
    TransformerFactory.newInstance().newTransformer().transform(xmlSource, result)
    return outputStream.toByteArray()
}

fun Node.getFirstChildElement(): Element {
    var child = this.firstChild
    while (child != null && child.nodeType != Node.ELEMENT_NODE) child = child.nextSibling
    return child as Element
}

fun Instant.toXMLGregorianCalendar(): XMLGregorianCalendar =
    DatatypeFactory.newInstance().newXMLGregorianCalendar(
        GregorianCalendar().also { it.setTimeInMillis(this.toEpochMilli()) }
    )
