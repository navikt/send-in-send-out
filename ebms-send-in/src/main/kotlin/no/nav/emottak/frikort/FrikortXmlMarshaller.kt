package no.nav.emottak.frikort

import no.nav.emottak.util.XmlMarshaller
import javax.xml.bind.JAXBContext

private val commonClasses = listOf(
    no.kith.xmlstds.msghead._2006_05_24.ObjectFactory::class.java
)

private val xmldsigClasses = listOf(
    org.w3._1999.xlink.ObjectFactory::class.java,
    org.w3._2009.xmldsig11_.ObjectFactory::class.java
)

private val egenandelForesporselClasses = listOf(
    no.kith.xmlstds.nav.egenandel._2010_02_01.ObjectFactory::class.java
)

private val egenandelForesporselV2Classes = listOf(
    no.kith.xmlstds.nav.egenandel._2016_06_10.ObjectFactory::class.java
)

private val egenandelMengdeForesporselClasses = listOf(
    no.kith.xmlstds.nav.egenandelmengde._2010_10_06.ObjectFactory::class.java,
    no.kith.xmlstds.nav.egenandelmengde._2016_06_10.ObjectFactory::class.java
)

private val frikortSporringClasses = listOf(
    no.nav.tjeneste.ekstern.frikort.v1.types.FrikortsporringMengdeRequest::class.java,
    no.nav.tjeneste.ekstern.frikort.v1.types.FrikortsporringMengdeResponse::class.java,
    no.nav.tjeneste.ekstern.frikort.v1.types.FrikortsporringRequest::class.java,
    no.nav.tjeneste.ekstern.frikort.v1.types.FrikortsporringResponse::class.java,
    org.xmlsoap.schemas.soap.envelope.ObjectFactory::class.java,
    no.trygdeetaten.xml.eiff._1.ObjectFactory::class.java
)

private val frikortNamespacePrefixMapper = FrikortNamespacePrefixMapper()

val frikortSporringXmlMarshaller = XmlMarshaller(
    JAXBContext.newInstance(
        *(
            commonClasses +
                xmldsigClasses +
                frikortSporringClasses +
                egenandelForesporselClasses +
                egenandelForesporselV2Classes +
                egenandelMengdeForesporselClasses
            ).toTypedArray()
    )
)

val egenandelForesporselXmlMarshaller = XmlMarshaller(
    JAXBContext.newInstance(
        *(commonClasses + egenandelForesporselClasses).toTypedArray()
    ),
    frikortNamespacePrefixMapper
)

val egenandelForesporselV2XmlMarshaller = XmlMarshaller(
    JAXBContext.newInstance(
        *(commonClasses + egenandelForesporselV2Classes).toTypedArray()
    ),
    frikortNamespacePrefixMapper
)

val egenandelForesporselFullXmlMarshaller = XmlMarshaller(
    JAXBContext.newInstance(
        *(commonClasses + egenandelForesporselClasses + egenandelForesporselV2Classes).toTypedArray()
    ),
    frikortNamespacePrefixMapper
)

val egenandelMengdeForesporselXmlMarshaller = XmlMarshaller(
    JAXBContext.newInstance(
        *(commonClasses + xmldsigClasses + egenandelMengdeForesporselClasses).toTypedArray()
    ),
    frikortNamespacePrefixMapper
)
