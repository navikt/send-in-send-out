package no.nav.emottak.pasientliste

import no.nav.emottak.util.XmlMarshaller
import javax.xml.bind.JAXBContext

val PasientlisteXmlMarshaller = XmlMarshaller(
    JAXBContext.newInstance(
        no.kith.xmlstds.nav.pasientliste._2010_02_01.ObjectFactory::class.java,
        no.kith.xmlstds.msghead._2006_05_24.ObjectFactory::class.java,
        org.xmlsoap.schemas.soap.envelope.ObjectFactory::class.java,
        org.w3._1999.xlink.ObjectFactory::class.java,
        org.w3._2009.xmldsig11_.ObjectFactory::class.java,
        no.trygdeetaten.xml.eiff._1.ObjectFactory::class.java
    )
)
