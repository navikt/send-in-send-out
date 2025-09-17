package no.nav.emottak.frikort

import no.kith.xmlstds.nav.egenandel._2010_02_01.EgenandelForesporsel
import no.kith.xmlstds.nav.egenandel._2010_02_01.EgenandelSvar
import no.kith.xmlstds.nav.egenandel._2016_06_10.EgenandelForesporselV2
import no.kith.xmlstds.nav.egenandel._2016_06_10.EgenandelSvarV2
import no.kith.xmlstds.nav.egenandelmengde._2010_10_06.EgenandelMengdeForesporsel
import no.kith.xmlstds.nav.egenandelmengde._2010_10_06.EgenandelMengdeSvar
import no.kith.xmlstds.nav.egenandelmengde._2016_06_10.EgenandelMengdeForesporselV2
import no.kith.xmlstds.nav.egenandelmengde._2016_06_10.EgenandelMengdeSvarV2
import no.nav.emottak.util.XmlMarshaller
import javax.xml.bind.JAXBContext

private val egenandelForesporselClasses = listOf(
    EgenandelForesporsel::class.java,
    EgenandelSvar::class.java,
    EgenandelForesporselV2::class.java,
    EgenandelSvarV2::class.java
)

private val egenandelMengdeForesporselClasses = listOf(
    EgenandelMengdeForesporsel::class.java,
    EgenandelMengdeSvar::class.java,
    EgenandelMengdeForesporselV2::class.java,
    EgenandelMengdeSvarV2::class.java
)

private val commonClasses = listOf(
    no.kith.xmlstds.msghead._2006_05_24.ObjectFactory::class.java,
    org.w3._1999.xlink.ObjectFactory::class.java,
    org.w3._2009.xmldsig11_.ObjectFactory::class.java
)

val egenandelForesporselXmlMarshaller = XmlMarshaller(
    JAXBContext.newInstance(
        *(commonClasses + egenandelForesporselClasses).toTypedArray()
    )
)

val egenandelMengdeForesporselXmlMarshaller = XmlMarshaller(
    JAXBContext.newInstance(
        *(commonClasses + egenandelMengdeForesporselClasses).toTypedArray()
    )
)
