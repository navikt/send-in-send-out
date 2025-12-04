package no.nav.emottak.pasientliste

import jakarta.xml.bind.JAXBContext
import no.nav.emottak.util.XmlMarshaller

val PasientlisteXmlMarshaller = XmlMarshaller(
    JAXBContext.newInstance(
        no.kith.xmlstds.nav.pasientliste._2010_02_01.ObjectFactory::class.java,
        no.kith.xmlstds.msghead._2006_05_24.ObjectFactory::class.java,
        no.trygdeetaten.xml.eiff._1.ObjectFactory::class.java
    )
)
