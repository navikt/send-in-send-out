package no.nav.emottak

import kotlinx.datetime.Instant
import no.nav.emottak.fellesformat.FellesformatXmlBuilder
import no.nav.emottak.fellesformat.asEIFellesFormat_Legemelding
import no.nav.emottak.utils.common.model.Addressing
import no.nav.emottak.utils.common.model.EbmsProcessing
import no.nav.emottak.utils.common.model.Party
import no.nav.emottak.utils.common.model.PartyId
import no.nav.emottak.utils.common.model.SendInRequest
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.util.GregorianCalendar
import java.util.TimeZone
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class LegemeldingRequestTest {

    // Verify that a given Legemelding request is converted to the expected Fellesformat XML string

    @Test
    fun verifyRequestAsXml_withBuilder() {
        // Set up request with values that fit example file legemelding.xml
        // Had to tweak namespaces, and sort attributes alphabetically
        val request = SendInRequest(
            messageId = "ed63e4e0-6bed-43b1-b99d-74ef5cb2bc47", conversationId = "1234",
            requestId = "dummy", payloadId = "dummy", cpaId = "", partnerId = 0, ebmsProcessing = EbmsProcessing(),
            signedOf = "20086600138", payload = "".toByteArray(),
            addressing = Addressing(
                service = "Legemelding",
                action = "Legeerklaring",
                from = Party(role = "Lege", partyId = listOf(PartyId("orgnummer", "12345678910"))),
                to = Party(role = "dummy", partyId = listOf())
            )
        )
        // Perform conversion to XMl and override the generated timestamp with value from legemelding.xml
        val fellesformat = request.asEIFellesFormat_Legemelding()
        val timestamp: Instant = Instant.parse("2026-04-08T00:00:00.000+02:00")
        fellesformat.mottakenhetBlokk.mottattDatotid = toXmlGregorianCalendar(timestamp)
        val builder = FellesformatXmlBuilder()

        // Verify that it works OK also with prolog
        val completePayload = """<?xml version="1.0" encoding="UTF-8"?>""" + payloadFromExpectedXmlFile
        val xml = builder.buildXmlWithCustomMottakenhetBlokk(fellesformat.mottakenhetBlokk, completePayload.toByteArray())

        // Verify that we get expected XML (remove whitespace) V3 has compact ns definitions, and sorted attributes
        val expectedXml = this::class.java.classLoader.getResourceAsStream("legemeldingV3.xml")!!.readAllBytes().decodeToString()
        assertEquals(removeWhitespaceBetweenXmlElementsAndMinimiseOtherWhitespace(expectedXml), removeWhitespaceBetweenXmlElementsAndMinimiseOtherWhitespace(xml))
    }

    @Test
    fun verifyRequestAsXml_withBuilderForCompleteDocument() {
        // Set up request with values that fit example file legemelding.xml
        // Had to tweak namespaces, and sort attributes alphabetically
        val request = SendInRequest(
            messageId = "ed63e4e0-6bed-43b1-b99d-74ef5cb2bc47", conversationId = "1234",
            requestId = "dummy", payloadId = "dummy", cpaId = "", partnerId = 0, ebmsProcessing = EbmsProcessing(),
            signedOf = "20086600138", payload = "".toByteArray(),
            addressing = Addressing(
                service = "Legemelding",
                action = "Legeerklaring",
                from = Party(role = "Lege", partyId = listOf(PartyId("orgnummer", "12345678910"))),
                to = Party(role = "dummy", partyId = listOf())
            )
        )
        // Perform conversion to XMl and override the generated timestamp with value from legemelding.xml
        val fellesformat = request.asEIFellesFormat_Legemelding()
        val timestamp: Instant = Instant.parse("2026-04-08T00:00:00.000+02:00")
        fellesformat.mottakenhetBlokk.mottattDatotid = toXmlGregorianCalendar(timestamp)
        val builder = FellesformatXmlBuilder()

        // Verify that it works OK also with prolog
        val completePayload = """<?xml version="1.0" encoding="UTF-8"?>""" + payloadFromExpectedXmlFile
        val xml = builder.buildXml(fellesformat.mottakenhetBlokk, completePayload.toByteArray())

        // Verify that we get expected XML (remove whitespace) V4 as V3 but mottakenhetblokk has sorted attributes
        val expectedXml = this::class.java.classLoader.getResourceAsStream("legemeldingV4.xml")!!.readAllBytes().decodeToString()
        assertEquals(removeWhitespaceBetweenXmlElementsAndMinimiseOtherWhitespace(expectedXml), removeWhitespaceBetweenXmlElementsAndMinimiseOtherWhitespace(xml))
    }

    fun toXmlGregorianCalendar(timestamp: Instant): XMLGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(
        GregorianCalendar(TimeZone.getTimeZone(ZoneId.of("+02:00"))).apply { this.setTimeInMillis(timestamp.toEpochMilliseconds()) }
    )

    val payloadFromExpectedXmlFile = """
    <MsgHead xmlns="http://www.kith.no/xmlstds/msghead/2006-05-24">
        <MsgInfo>
            <Type V="Legerkl.ring ved arbeidsuf.rhet" DN="LEGEERKL"/>
            <MIGversion>v1.2 2006-05-24</MIGversion>
            <GenDate>2026-04-08T09:12:20.232947798</GenDate>
            <MsgId>312e6634-39f8-4701-bfbc-c322089617db</MsgId>
            <Sender>
                <Organisation>
                    <OrganisationName>Kule helsetjenester AS</OrganisationName>
                    <Ident>
                        <Id>223456789</Id>
                        <TypeId V="ENH" S="1.16.578.1.12.3.1.1.9051" DN="Organisasjonsnummeret i Enhetsregister (Brønnøysund)"/>
                    </Ident>
                    <Ident>
                        <Id>0123</Id>
                        <TypeId V="HER" S="1.23.456.7.89.1.2.3.4567.8912" DN="Identifikator fra Helsetjenesteenhetsregisteret (HER-id)"/>
                    </Ident>
                    <Address>
                        <StreetAdr>Oppdiktet gate 203</StreetAdr>
                        <PostalCode>1234</PostalCode>
                        <City>Oslo</City>
                    </Address>
                    <Organisation/>
                    <HealthcareProfessional>
                        <FamilyName>FLOSKEL</FamilyName>
                        <MiddleName>MÅPENDE</MiddleName>
                        <GivenName>TRIVIELL</GivenName>
                        <Ident>
                            <Id>20086600138</Id>
                            <TypeId V="FNR" S="6.87.654.3.21.9.8.7.6543.2198" DN="Fødselsnummer"/>
                        </Ident>
                    </HealthcareProfessional>
                </Organisation>
            </Sender>
            <Receiver>
                <Organisation>
                    <OrganisationName>NAV</OrganisationName>
                    <Ident>
                        <Id>12668</Id>
                        <TypeId V="HER" S="1.23.456.7.89.1.2.3.4567.8912" DN="Identifikator fra Helsetjenesteenhetsregisteret (HER-id)"/>
                    </Ident>
                    <Ident>
                        <Id>123456789</Id>
                        <TypeId V="ENH" S="1.16.578.1.12.3.1.1.9051" DN="Organisasjonsnummeret i Enhetsregister (Brønnøysund)"/>
                    </Ident>
                    <Address>
                        <StreetAdr></StreetAdr>
                        <PostalCode></PostalCode>
                        <City></City>
                    </Address>
                </Organisation>
            </Receiver>
            <Patient>
                <FamilyName>Duck</FamilyName>
                <GivenName>Donald</GivenName>
                <Ident>
                    <Id>12345678910</Id>
                    <TypeId V="FNR" S="2.16.578.1.12.4.1.1.8116" DN="Fødselsnummer"/>
                </Ident>
            </Patient>
        </MsgInfo>
        <Document>
            <RefDoc>
                <MsgType V="XML" DN="XML-instans"/>
                <Content>
                    <Legeerklaring>
                        <LegeerklaringGjelder typeLegeerklaring="2"/>
                        <Pasientopplysninger>
                            <Pasient fodselsnummer="04056600324">
                                <Navn>
                                    <Etternavn>FALLSKJERM</Etternavn>
                                    <Fornavn>VAKKER</Fornavn>
                                </Navn>
                                <Arbeidsforhold primartArbeidsforhold="1" yrkeskode="Utvikler">
                                    <Virksomhet organisasjonsnummer="133144" virksomhetsBetegnelse="NAV IKT">
                                        <VirksomhetsAdr adressetype="ABC"/>
                                    </Virksomhet>
                                </Arbeidsforhold>
                            </Pasient>
                        </Pasientopplysninger>
                        <PlanUtredBehandle>
                            <HenvistUtredning antattVentetid="3" henvistDato="2017-11-05+01:00">
                                <Spesifikasjon>Dra på fisketur med en guide</Spesifikasjon>
                            </HenvistUtredning>
                            <NyeLegeopplysninger>Den gamle planen fungerte ikke</NyeLegeopplysninger>
                            <NyVurdering>Trenger ikke ny vurdering</NyVurdering>
                            <BehandlingsPlan>Trenger å slappe av med litt fisking</BehandlingsPlan>
                            <UtredningsPlan>Burde dra ut på fisketur for å slappe av</UtredningsPlan>
                        </PlanUtredBehandle>
                        <DiagnoseArbeidsuforhet>
                            <DiagnoseKodesystem kodesystem="1">
                                <Enkeltdiagnose diagnose="Vondt i skulder" kodeverdi="H100" sortering="0"/>
                            </DiagnoseKodesystem>
                            <VurderingYrkesskade borVurderes="1" skadeDato="2026-04-08+02:00"/>
                            <StatusPresens/>
                            <SymptomerBehandling>Får vondt av behandlingen</SymptomerBehandling>
                        </DiagnoseArbeidsuforhet>
                        <ForslagTiltak tiltak="2">
                            <AktueltTiltak typeTiltak="1">
                                <HvilkeAndreTiltak/>
                            </AktueltTiltak>
                            <Opplysninger/>
                            <BegrensningerTiltak>Trenger lettere arbeid</BegrensningerTiltak>
                        </ForslagTiltak>
                        <VurderingFunksjonsevne>
                            <Arbeidssituasjon annenArbeidssituasjon="" arbeidssituasjon="4"/>
                            <VurderingArbeidsevne gjenopptaArbeid="2" narGjenopptaArbeid="1" narTaAnnetArbeid="1" taAnnetArbeid="1">
                                <IkkeGjore>Ikke tunge løft</IkkeGjore>
                                <HensynAnnetYrke>Ingen dans</HensynAnnetYrke>
                            </VurderingArbeidsevne>
                            <Funksjonsevne>Kan ikke lengre danse</Funksjonsevne>
                            <KravArbeid>Ingen dans</KravArbeid>
                        </VurderingFunksjonsevne>
                        <Prognose bedreArbeidsevne="2">
                            <AntattVarighet>Ikke varig.</AntattVarighet>
                            <VarighetFunksjonsnedsettelse>Ikke varig.</VarighetFunksjonsnedsettelse>
                            <VarighetNedsattArbeidsevne>Ikke varig.</VarighetNedsattArbeidsevne>
                        </Prognose>
                        <ArsakssammenhengLegeerklaring>Vedkommende har vært syk lenge, duplikatbuster: b2fefa18-c34e-4143-828c-f6484e62a079</ArsakssammenhengLegeerklaring>
                        <ForbeholdLegeerklaring tilbakeholdInnhold="2"/>
                        <AndreOpplysninger onskesKopi="1">
                            <Opplysning>Andre opplysninger</Opplysning>
                        </AndreOpplysninger>
                    </Legeerklaring>
                </Content>
            </RefDoc>
        </Document>
    </MsgHead>
        """
}
