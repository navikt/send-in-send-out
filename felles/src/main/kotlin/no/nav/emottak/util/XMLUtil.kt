package no.nav.emottak.util

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
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList

const val BIRTHDAY: Int = 6
const val FNUMBER: Int = 11

fun refParam(nodeList: NodeList, tagName: String): String {
    for (count in 0 until nodeList.length) {
        val elemNode = nodeList.item(count)
        if (elemNode.nodeType == Node.ELEMENT_NODE) {
            if (elemNode.nodeName.contains(tagName) ) {
                return elemNode.textContent
            }

            val attr: String = refParam(elemNode.childNodes, tagName)
            if (attr != "NA") {
                return attr
            }
        }
    }
    return "NA"
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
