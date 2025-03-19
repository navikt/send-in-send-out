package no.nav.emottak.util

import no.kith.xmlstds.nav.egenandel._2010_02_01.EgenandelForesporsel
import no.kith.xmlstds.nav.egenandel._2016_06_10.EgenandelForesporselV2
import no.kith.xmlstds.nav.egenandelmengde._2010_10_06.EgenandelMengdeForesporsel
import no.kith.xmlstds.nav.egenandelmengde._2016_06_10.EgenandelMengdeForesporselV2
import no.kith.xmlstds.nav.pasientliste._2010_02_01.PasientlisteForesporsel
import no.nav.ekstern.virkemiddelokonomi.tjenester.utbetaling.v1.FinnUtbetalingListe
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

fun refParamFrikort(foresporsel: Any): String {
    return when (foresporsel) {
        is EgenandelForesporselV2 -> {
            foresporsel.harBorgerEgenandelfritak?.borgerFnr ?: foresporsel.harBorgerFrikort?.borgerFnr ?: "NA"
        }
        is EgenandelForesporsel -> {
            foresporsel.harBorgerEgenandelfritak?.borgerFnr ?: foresporsel.harBorgerFrikort?.borgerFnr ?: "NA"
        }
        is EgenandelMengdeForesporselV2 -> {
            foresporsel.harBorgerFrikort?.size.toString() ?: "NA"
        }
        is EgenandelMengdeForesporsel -> {
            foresporsel.harBorgerFrikort?.size.toString() ?: "NA"
        }
        else -> "NA"
    }
}

fun refParam(fellesformat: EIFellesformat): String {
    val foresporsel = fellesformat.msgHead.document.first().refDoc.content.any.first()

    return when (foresporsel) {
        is PasientlisteForesporsel -> {
            foresporsel.hentPasientliste?.fnrLege ?: "NA"
        }
        is FinnUtbetalingListe -> {
            foresporsel.request.bruker?.brukerId ?: "NA"
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
