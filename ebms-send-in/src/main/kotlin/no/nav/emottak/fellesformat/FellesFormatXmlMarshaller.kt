package no.nav.emottak.fellesformat

import no.nav.emottak.util.XmlMarshaller
import javax.xml.bind.JAXBContext

val FellesFormatXmlMarshaller = XmlMarshaller(
/* NB! Forsiktig med å marshalle fagmeldingen.
    Pga. hardkoding hos klienter kan det brekke selv med GYLDIG XML output (f.eks. elementers/props/attributters rekkefølge),
    Velger derfor å ikke mate objectfactory for fagmeldingene til FellesFormatMarshalleren fordi vi har ingen
    garanti for at rekkefølge ikke muteres
    Bruk heller MessageContentMarshaller om du må ha tak i informasjon fra
    fagmelding og ikke konverter det objektet tilbake til bytes
    Bug i prod på dette 17 sept 2025
 */
    JAXBContext.newInstance(
        org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ObjectFactory::class.java,
        org.xmlsoap.schemas.soap.envelope.ObjectFactory::class.java,
        org.w3._1999.xlink.ObjectFactory::class.java,
        org.w3._2009.xmldsig11_.ObjectFactory::class.java,
        no.trygdeetaten.xml.eiff._1.ObjectFactory::class.java,
        no.kith.xmlstds.msghead._2006_05_24.ObjectFactory::class.java
    )
)

val MessageContentMarshaller = XmlMarshaller(
    JAXBContext.newInstance(
        org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ObjectFactory::class.java,
        org.xmlsoap.schemas.soap.envelope.ObjectFactory::class.java,
        org.w3._1999.xlink.ObjectFactory::class.java,
        org.w3._2009.xmldsig11_.ObjectFactory::class.java,
        no.trygdeetaten.xml.eiff._1.ObjectFactory::class.java,
        no.kith.xmlstds.msghead._2006_05_24.ObjectFactory::class.java,
        no.nav.tjeneste.ekstern.frikort.v1.types.ObjectFactory::class.java,
        no.nav.ekstern.virkemiddelokonomi.tjenester.utbetaling.v1.ObjectFactory::class.java,
        no.kith.xmlstds.nav.pasientliste._2010_02_01.ObjectFactory::class.java,
        no.kith.xmlstds.nav.egenandel._2010_02_01.ObjectFactory::class.java,
        no.kith.xmlstds.nav.egenandel._2016_06_10.ObjectFactory::class.java,
        no.kith.xmlstds.nav.egenandelmengde._2016_06_10.ObjectFactory::class.java,
        no.kith.xmlstds.nav.egenandelmengde._2010_10_06.ObjectFactory::class.java
    )
)

fun marshal(objekt: Any) = FellesFormatXmlMarshaller.marshal(objekt)

fun <T> unmarshal(xml: String, clazz: Class<T>) = FellesFormatXmlMarshaller.unmarshal(xml, clazz)
