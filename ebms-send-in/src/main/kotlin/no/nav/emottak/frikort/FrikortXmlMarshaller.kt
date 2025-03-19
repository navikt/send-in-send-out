package no.nav.emottak.frikort

import no.kith.xmlstds.nav.egenandel._2016_06_10.EgenandelForesporselV2
import no.kith.xmlstds.nav.egenandel._2016_06_10.EgenandelSvarV2
import no.kith.xmlstds.nav.egenandelmengde._2016_06_10.EgenandelMengdeForesporselV2
import no.kith.xmlstds.nav.egenandelmengde._2016_06_10.EgenandelMengdeSvarV2
import no.nav.tjeneste.ekstern.frikort.v1.types.FrikortsporringMengdeRequest
import no.nav.tjeneste.ekstern.frikort.v1.types.FrikortsporringMengdeResponse
import java.io.StringWriter
import javax.xml.bind.JAXBContext
import javax.xml.stream.XMLInputFactory

val FrikortXmlMarshaller = XmlMarshaller()

fun marshal(objekt: Any) = FrikortXmlMarshaller.marshal(objekt)
fun <T> unmarshal(xml: String, clazz: Class<T>): T = FrikortXmlMarshaller.unmarshal(xml, clazz)

class XmlMarshaller {

    companion object {
        private val jaxbContext = JAXBContext.newInstance(
            FrikortsporringMengdeRequest::class.java,
            EgenandelForesporselV2::class.java,
            EgenandelMengdeForesporselV2::class.java,
            FrikortsporringMengdeResponse::class.java,
            EgenandelSvarV2::class.java,
            EgenandelMengdeSvarV2::class.java,
            org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ObjectFactory::class.java,
            org.xmlsoap.schemas.soap.envelope.ObjectFactory::class.java,
            no.kith.xmlstds.msghead._2006_05_24.ObjectFactory::class.java,
            org.w3._1999.xlink.ObjectFactory::class.java,
            org.w3._2009.xmldsig11_.ObjectFactory::class.java
        )
        private val marshaller = jaxbContext.createMarshaller()
        private val unmarshaller = jaxbContext.createUnmarshaller()
        private val marshlingMonitor = Any()
        private val unmarshlingMonitor = Any()
    }

    fun marshal(objekt: Any): String {
        val writer = StringWriter()
        synchronized(marshlingMonitor) {
            marshaller.marshal(objekt, writer)
        }
        return writer.toString()
    }

    fun <T> unmarshal(xml: String, clazz: Class<T>): T {
        val reader = XMLInputFactory.newInstance().createXMLStreamReader(xml.reader())
        return synchronized(unmarshlingMonitor) {
            unmarshaller.unmarshal(reader, clazz).value
        }
    }
}
