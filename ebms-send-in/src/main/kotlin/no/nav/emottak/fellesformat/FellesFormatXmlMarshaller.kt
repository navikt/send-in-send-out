package no.nav.emottak.fellesformat

import no.nav.emottak.util.XmlMarshaller
import javax.xml.bind.JAXBContext

val FellesFormatXmlMarshaller = XmlMarshaller(
    JAXBContext.newInstance(
        org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ObjectFactory::class.java,
        org.xmlsoap.schemas.soap.envelope.ObjectFactory::class.java,
        org.w3._1999.xlink.ObjectFactory::class.java,
        org.w3._2009.xmldsig11_.ObjectFactory::class.java,
        no.trygdeetaten.xml.eiff._1.ObjectFactory::class.java,
        no.kith.xmlstds.msghead._2006_05_24.ObjectFactory::class.java,
        no.nav.tjeneste.ekstern.frikort.v1.types.ObjectFactory::class.java,
        no.kith.xmlstds.nav.pasientliste._2010_02_01.ObjectFactory::class.java,
        no.kith.xmlstds.nav.pasientliste._2010_02_01.PasientlisteForesporsel::class.java
    )
)

fun marshal(objekt: Any) = FellesFormatXmlMarshaller.marshal(objekt)

fun <T> unmarshal(xml: String, clazz: Class<T>) = FellesFormatXmlMarshaller.unmarshal(xml, clazz)
