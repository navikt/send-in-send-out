package no.nav.emottak.frikort

import no.kith.xmlstds.nav.egenandel._2016_06_10.EgenandelSvarV2
import no.kith.xmlstds.nav.egenandelmengde._2016_06_10.EgenandelMengdeSvarV2
import no.nav.emottak.util.XmlMarshaller
import no.nav.tjeneste.ekstern.frikort.v1.types.FrikortsporringMengdeRequest
import no.nav.tjeneste.ekstern.frikort.v1.types.FrikortsporringMengdeResponse
import javax.xml.bind.JAXBContext

val FrikortXmlMarshaller = XmlMarshaller(
    JAXBContext.newInstance(
        FrikortsporringMengdeRequest::class.java,
        FrikortsporringMengdeResponse::class.java,
        EgenandelSvarV2::class.java,
        EgenandelMengdeSvarV2::class.java,
        org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ObjectFactory::class.java,
        org.xmlsoap.schemas.soap.envelope.ObjectFactory::class.java,
        no.kith.xmlstds.msghead._2006_05_24.ObjectFactory::class.java,
        org.w3._1999.xlink.ObjectFactory::class.java,
        org.w3._2009.xmldsig11_.ObjectFactory::class.java,
        no.kith.xmlstds.nav.pasientliste._2010_02_01.PasientlisteForesporsel::class.java,
    )
)

fun marshal(objekt: Any) = FrikortXmlMarshaller.marshal(objekt)

fun <T> unmarshal(xml: String, clazz: Class<T>) = FrikortXmlMarshaller.unmarshal(xml, clazz)
