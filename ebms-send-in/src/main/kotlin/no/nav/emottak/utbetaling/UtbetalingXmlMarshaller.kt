package no.nav.emottak.utbetaling

import jakarta.xml.bind.JAXBContext
import no.nav.ekstern.virkemiddelokonomi.tjenester.utbetaling.meldinger.v1.FinnBrukersUtbetalteYtelserRequest
import no.nav.ekstern.virkemiddelokonomi.tjenester.utbetaling.meldinger.v1.FinnUtbetalingListeRequest
import no.nav.ekstern.virkemiddelokonomi.tjenester.utbetaling.v1.FinnUtbetalingListe
import no.nav.ekstern.virkemiddelokonomi.tjenester.utbetaling.v1.FinnUtbetalingListeResponse
import no.nav.emottak.util.XmlMarshaller

val UtbetalingXmlMarshaller = XmlMarshaller(
    JAXBContext.newInstance(
        FinnUtbetalingListeRequest::class.java,
        FinnBrukersUtbetalteYtelserRequest::class.java,
        FinnUtbetalingListe::class.java,
        FinnUtbetalingListeResponse::class.java,
        no.nav.ekstern.virkemiddelokonomi.tjenester.utbetaling.meldinger.v1.ObjectFactory::class.java,
        no.nav.ekstern.virkemiddelokonomi.tjenester.utbetaling.feil.v1.ObjectFactory::class.java,
        no.nav.ekstern.virkemiddelokonomi.tjenester.utbetaling.v1.ObjectFactory::class.java,
        no.kith.xmlstds.msghead._2006_05_24.ObjectFactory::class.java
    )
)

fun marshal(objekt: Any) = UtbetalingXmlMarshaller.marshal(objekt)

fun <T> unmarshal(xml: String, clazz: Class<T>) = UtbetalingXmlMarshaller.unmarshal(xml, clazz)
