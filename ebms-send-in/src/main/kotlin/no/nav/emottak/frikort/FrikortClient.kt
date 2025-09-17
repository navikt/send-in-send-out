package no.nav.emottak.frikort

import no.nav.emottak.cxf.ServiceBuilder
import no.nav.emottak.fellesformat.FellesFormatXmlMarshaller
import no.nav.emottak.log
import no.nav.emottak.utils.environment.getEnvVar
import no.nav.emottak.utils.environment.getSecret
import no.nav.tjeneste.ekstern.frikort.v1.FrikortV1Port
import no.nav.tjeneste.ekstern.frikort.v1.types.FrikortsporringMengdeResponse
import no.nav.tjeneste.ekstern.frikort.v1.types.FrikortsporringResponse
import no.nav.tjeneste.ekstern.frikort.v1.types.ObjectFactory
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import javax.xml.namespace.QName

val frikortEndpoint = frikortEndpoint()
private val frikortObjectFactory = ObjectFactory()

fun frikortEndpoint(): FrikortV1Port =
    ServiceBuilder(FrikortV1Port::class.java)
        .withAddress(getEnvVar("FRIKORT_URL", "https://wasapp-local.adeo.no/nav-frikort/tjenestereksterne"))
        .withWsdl("classpath:frikort_v1.wsdl")
        .withServiceName(QName("http://nav.no/tjeneste/ekstern/frikort/v1", "Frikort_v1Service"))
        .withEndpointName(QName("http://nav.no/tjeneste/ekstern/frikort/v1", "Frikort_v1Port"))
        .withFrikortContextClasses()
        .build()
        .withBasicSecurity(
            getSecret("/secret/serviceuser/username", "testUsername"),
            getSecret("/secret/serviceuser/password", "testPassword")
        )
        .get()

fun frikortsporring(fellesformat: EIFellesformat): FrikortsporringResponse {
    log.debug(
        "Sending in frikortsporring request with body: " + FellesFormatXmlMarshaller.marshal(fellesformat)
    )

    return frikortEndpoint.frikortsporring(
        frikortObjectFactory.createFrikortsporringRequest().also { it.eiFellesformat = fellesformat }
    ).also {
        log.debug("Send in Frikort response " + FellesFormatXmlMarshaller.marshal(it))
    }
}

fun frikortsporringMengde(fellesformat: EIFellesformat): FrikortsporringMengdeResponse {
    log.debug(
        "Sending in frikortsporringMengde request with body: " + FellesFormatXmlMarshaller.marshal(fellesformat)
    )

    return frikortEndpoint.frikortsporringMengde(
        frikortObjectFactory.createFrikortsporringMengdeRequest().also { it.eiFellesformat = fellesformat }
    ).also {
        log.debug("Send in FrikortMengde response " + FellesFormatXmlMarshaller.marshal(it))
    }
}
