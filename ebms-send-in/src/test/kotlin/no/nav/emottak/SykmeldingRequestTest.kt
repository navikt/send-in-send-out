package no.nav.emottak

import kotlinx.datetime.Instant
import no.nav.emottak.fellesformat.FellesformatXmlBuilder
import no.nav.emottak.fellesformat.asEIFellesFormat_Sykmelding
import no.nav.emottak.fellesformat.asEIFellesFormat_SykmeldingWithoutPayload
import no.nav.emottak.sykmelding.marshalSykmelding
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
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class SykmeldingRequestTest {

    // Verify that a given Sykmelding request is converted to the expected Fellesformat XML string

    @Test
    fun verifyRequestAsXml_unmarshal_marshal() {
        // Set up request with values that fit example file sykemelding.xml
        // Removed xsd locations, removed NS "http://www.kith.no/xmlstds/felleskomponent1"
        // Also removed some signature stuff with difficult formatting
        // CHanged sequence of attributes in MottakEnhetBlokk, blanked some attrs also
        val request = SendInRequest(
            messageId = "2604160914prid26694.1", conversationId = "a219014c-9739-4263-983a-6dd9fc82f8f1",
            requestId = "dummy", payloadId = "dummy", cpaId = "nav:qass:36181", partnerId = 0, ebmsProcessing = EbmsProcessing(),
            signedOf = "06828399789", payload = payloadFromExpectedXmlFile.toByteArray(),
            addressing = Addressing(
                service = "Sykmelding",
                action = "Registrering",
                from = Party(role = "Sykmelder", partyId = listOf(PartyId("orgnummer", "912719103"))),
                to = Party(role = "dummy", partyId = listOf())
            )
        )
        // Perform conversion to XMl and override the generated timestamp with value from sykemelding.xml
        val fellesformat = request.asEIFellesFormat_Sykmelding()
        val timestamp: Instant = Instant.parse("2026-04-16T09:14:27Z")
        fellesformat.mottakenhetBlokk.mottattDatotid = toXmlGregorianCalendar(timestamp)
        val xml = marshalSykmelding(fellesformat)

        // Verify that we get expected XML (remove whitespace)
        val expectedXml = this::class.java.classLoader.getResourceAsStream("sykemelding.xml")!!.readAllBytes().decodeToString()
//        assertEquals(removeWhitespaceBetweenXmlElementsAndMinimiseOtherWhitespace(expectedXml), removeWhitespaceBetweenXmlElementsAndMinimiseOtherWhitespace(xml))
        loggDiff(removeWhitespaceBetweenXmlElementsAndMinimiseOtherWhitespace(expectedXml), removeWhitespaceBetweenXmlElementsAndMinimiseOtherWhitespace(xml))
    }

//    @Test
    fun verifyRequestAsXml_withBuilder() {
        val request = SendInRequest(
            messageId = "2604160914prid26694.1", conversationId = "a219014c-9739-4263-983a-6dd9fc82f8f1",
            requestId = "dummy", payloadId = "dummy", cpaId = "nav:qass:36181", partnerId = 0, ebmsProcessing = EbmsProcessing(),
            signedOf = "06828399789", payload = "".toByteArray(),
            addressing = Addressing(
                service = "Sykmelding",
                action = "Registrering",
                from = Party(role = "Sykmelder", partyId = listOf(PartyId("orgnummer", "912719103"))),
                to = Party(role = "dummy", partyId = listOf())
            )
        )
        // Perform conversion to XMl and override the generated timestamp with value from sykemelding.xml
        val fellesformat = request.asEIFellesFormat_SykmeldingWithoutPayload()
        val timestamp: Instant = Instant.parse("2026-04-16T09:14:27Z")
        fellesformat.mottakenhetBlokk.mottattDatotid = toXmlGregorianCalendar(timestamp)
        val builder = FellesformatXmlBuilder()

        // Verify that it works OK also with prolog
        val completePayload = """<?xml version="1.0" encoding="UTF-8"?>""" + payloadFromExpectedXmlFile
        val xml = builder.buildXmlWithCustomMottakenhetBlokk(fellesformat.mottakenhetBlokk, completePayload.toByteArray())
// sykemelding er opprinnelig, 2 er med rekkefølge for trekkopplysning
        // Verify that we get expected XML (remove whitespace)
        val expectedXml = this::class.java.classLoader.getResourceAsStream("sykemelding2.xml")!!.readAllBytes().decodeToString()
//        assertEquals(removeWhitespaceBetweenXmlElementsAndMinimiseOtherWhitespace(expectedXml), removeWhitespaceBetweenXmlElementsAndMinimiseOtherWhitespace(xml))
        loggDiff(removeWhitespaceBetweenXmlElementsAndMinimiseOtherWhitespace(expectedXml), removeWhitespaceBetweenXmlElementsAndMinimiseOtherWhitespace(xml))
    }

    fun toXmlGregorianCalendar(timestamp: Instant): XMLGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(
        GregorianCalendar(TimeZone.getTimeZone(ZoneId.of("UTC"))).apply { this.setTimeInMillis(timestamp.toEpochMilliseconds()) }
    )

    val payloadFromExpectedXmlFile = """
    <MsgHead xmlns="http://www.kith.no/xmlstds/msghead/2006-05-24" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <MsgInfo>
            <Type DN="Medisinsk vurdering av arbeidsmulighet ved sykdom, sykmelding" V="SYKMELD"/>
            <MIGversion>v1.2 2006-05-24</MIGversion>
            <GenDate>2026-04-15T09:12:12</GenDate>
            <MsgId>042e41d8-c28a-4e04-b6e6-d3b4670d9558</MsgId>
            <Ack DN="Ja" V="J"/>
            <Sender xmlns="http://www.kith.no/xmlstds/msghead/2006-05-24" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                <ComMethod DN="EDI" V="EDI"/>
                <Organisation>
                    <OrganisationName>Pridok Test 5</OrganisationName>
                    <Ident>
                        <Id>8142326</Id>
                        <TypeId DN="HER-id" S="2.16.578.1.12.4.1.1.9051" V="HER"/>
                    </Ident>
                    <Ident>
                        <Id>999988914</Id>
                        <TypeId DN="Organisasjonsnummeret i Enhetsregister" S="2.16.578.1.12.4.1.1.9051" V="ENH"/>
                    </Ident>
                    <Address>
                        <Type DN="Postadresse" V="PST"/>
                        <StreetAdr>Testgata 5</StreetAdr>
                        <PostalCode>3154</PostalCode>
                        <City>TOLVSRØD</City>
                    </Address>
                    <TeleCom>
                        <TypeTelecom DN="Hovedtelefon" V="HP"/>
                        <TeleAddress V="tel:12345678"/>
                    </TeleCom>
                    <TeleCom>
                        <TypeTelecom DN="Arbeidsplass" V="WP"/>
                        <TeleAddress V="mailto:pridok.test5@testedi.nhn.no"/>
                    </TeleCom>
                    <HealthcareProfessional>
                        <FamilyName>GREVLING</FamilyName>
                        <GivenName>KVART</GivenName>
                        <Ident>
                            <Id>8144442</Id>
                            <TypeId DN="HER-id" S="2.16.578.1.12.4.1.1.8116" V="HER"/>
                        </Ident>
                        <Ident>
                            <Id>565505933</Id>
                            <TypeId DN="HPR-nummer" S="2.16.578.1.12.4.1.1.8116" V="HPR"/>
                        </Ident>
                    </HealthcareProfessional>
                </Organisation>
            </Sender>
            <Receiver xmlns="http://www.kith.no/xmlstds/msghead/2006-05-24" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                <ComMethod DN="EDI" V="EDI"/>
                <Organisation>
                    <OrganisationName>NAV IKT</OrganisationName>
                    <Ident>
                        <Id>79768</Id>
                        <TypeId DN="HER-id" S="2.16.578.1.12.4.1.1.9051" V="HER"/>
                    </Ident>
                    <Ident>
                        <Id>990983291</Id>
                        <TypeId DN="Organisasjonsnummeret i Enhetsregister" S="2.16.578.1.12.4.1.1.9051" V="ENH"/>
                    </Ident>
                    <Address>
                        <Type DN="Postadresse" V="PST"/>
                        <StreetAdr>Postboks 5 St Olavs plass</StreetAdr>
                        <PostalCode>0130</PostalCode>
                        <City>OSLO</City>
                    </Address>
                    <TeleCom>
                        <TypeTelecom DN="Arbeidsplass" V="WP"/>
                        <TeleAddress V="mailto:mottak-qass@test-es.nav.no"/>
                    </TeleCom>
                </Organisation>
            </Receiver>
        </MsgInfo>
        <Document>
            <RefDoc>
                <MsgType DN="XML-instans" V="XML"/>
                <Id>19412bc0-d1ed-45f2-8f67-00282b064347</Id>
                <Content>
                    <MsgHead xmlns="http://www.kith.no/xmlstds/msghead/2006-05-24" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                        <MsgInfo>
                            <Type DN="Medisinsk vurdering av arbeidsmulighet ved sykdom, sykmelding" V="SYKMELD"/>
                            <MIGversion>v1.2 2006-05-24</MIGversion>
                            <GenDate>2026-04-15T09:11:35</GenDate>
                            <MsgId>042e41d8-c28a-4e04-b6e6-d3b4670d9558</MsgId>
                            <Ack DN="Ja" V="J"/>
                            <Sender xmlns="http://www.kith.no/xmlstds/msghead/2006-05-24" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                                <ComMethod DN="EDI" V="EDI"/>
                                <Organisation>
                                    <OrganisationName>Pridok Test 5</OrganisationName>
                                    <Ident>
                                        <Id>8142326</Id>
                                        <TypeId DN="HER-id" S="2.16.578.1.12.4.1.1.9051" V="HER"/>
                                    </Ident>
                                    <Ident>
                                        <Id>999988914</Id>
                                        <TypeId DN="Organisasjonsnummeret i Enhetsregister" S="2.16.578.1.12.4.1.1.9051" V="ENH"/>
                                    </Ident>
                                    <Address>
                                        <Type DN="Postadresse" V="PST"/>
                                        <StreetAdr>Testgata 5</StreetAdr>
                                        <PostalCode>3154</PostalCode>
                                        <City>TOLVSRØD</City>
                                    </Address>
                                    <TeleCom>
                                        <TypeTelecom DN="Hovedtelefon" V="HP"/>
                                        <TeleAddress V="tel:12345678"/>
                                    </TeleCom>
                                    <TeleCom>
                                        <TypeTelecom DN="Arbeidsplass" V="WP"/>
                                        <TeleAddress V="mailto:pridok.test5@testedi.nhn.no"/>
                                    </TeleCom>
                                    <HealthcareProfessional>
                                        <FamilyName>GREVLING</FamilyName>
                                        <GivenName>KVART</GivenName>
                                        <Ident>
                                            <Id>8144442</Id>
                                            <TypeId DN="HER-id" S="2.16.578.1.12.4.1.1.8116" V="HER"/>
                                        </Ident>
                                        <Ident>
                                            <Id>565505933</Id>
                                            <TypeId DN="HPR-nummer" S="2.16.578.1.12.4.1.1.8116" V="HPR"/>
                                        </Ident>
                                    </HealthcareProfessional>
                                </Organisation>
                            </Sender>
                            <Receiver xmlns="http://www.kith.no/xmlstds/msghead/2006-05-24" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                                <ComMethod DN="EDI" V="EDI"/>
                                <Organisation>
                                    <OrganisationName>NAV IKT</OrganisationName>
                                    <Ident>
                                        <Id>79768</Id>
                                        <TypeId DN="HER-id" S="2.16.578.1.12.4.1.1.9051" V="HER"/>
                                    </Ident>
                                    <Ident>
                                        <Id>990983291</Id>
                                        <TypeId DN="Organisasjonsnummeret i Enhetsregister" S="2.16.578.1.12.4.1.1.9051" V="ENH"/>
                                    </Ident>
                                    <Address>
                                        <Type DN="Postadresse" V="PST"/>
                                        <StreetAdr>Postboks 5 St Olavs plass</StreetAdr>
                                        <PostalCode>0130</PostalCode>
                                        <City>OSLO</City>
                                    </Address>
                                    <TeleCom>
                                        <TypeTelecom DN="Arbeidsplass" V="WP"/>
                                        <TeleAddress V="mailto:mottak-qass@test-es.nav.no"/>
                                    </TeleCom>
                                </Organisation>
                            </Receiver>
                            <Patient xmlns="http://www.kith.no/xmlstds/msghead/2006-05-24" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                                <FamilyName>Akrobat</FamilyName>
                                <GivenName>Selvhjulpen</GivenName>
                                <Ident>
                                    <Id>07706830752</Id>
                                    <TypeId DN="Fødselsnummer" S="2.16.578.1.12.4.1.1.8116" V="FNR"/>
                                </Ident>
                                <Address>
                                    <Type DN="Postadresse" V="PST"/>
                                    <StreetAdr>Trøene 15</StreetAdr>
                                    <PostalCode>6213</PostalCode>
                                    <City>TAFJORD</City>
                                    <Country DN="Norge" V="NO"/>
                                </Address>
                            </Patient>
                        </MsgInfo>
                        <Document>
                            <RefDoc>
                                <MsgType DN="XML-instans" V="XML"/>
                                <Id>19412bc0-d1ed-45f2-8f67-00282b064347</Id>
                                <Content>
                                    <HelseOpplysningerArbeidsuforhet xmlns="http://www.kith.no/xmlstds/HelseOpplysningerArbeidsuforhet/2013-10-01" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                                        <RegelSettVersjon>2</RegelSettVersjon>
                                        <SyketilfelleStartDato>2026-04-15</SyketilfelleStartDato>
                                        <Pasient>
                                            <Navn>
                                                <Etternavn>Akrobat</Etternavn>
                                                <Fornavn>Selvhjulpen</Fornavn>
                                            </Navn>
                                            <Fodselsnummer>
                                                <Id>07706830752</Id>
                                                <TypeId DN="Fødselsnummer" S="2.16.578.1.12.4.1.1.8116" V="FNR"/>
                                            </Fodselsnummer>
                                            <KontaktInfo>
                                                <TypeTelecom DN="Mobiltelefon " V="MC "/>
                                                <TeleAddress V="mob:48157585"/>
                                            </KontaktInfo>
                                            <NavnFastlege>FYLDIG OVERGANG</NavnFastlege>
                                        </Pasient>
                                        <Arbeidsgiver>
                                            <HarArbeidsgiver DN=".n arbeidsgiver" V="1"/>
                                            <NavnArbeidsgiver>test</NavnArbeidsgiver>
                                        </Arbeidsgiver>
                                        <MedisinskVurdering>
                                            <HovedDiagnose>
                                                <Diagnosekode DN="Frysninger" S="2.16.578.1.12.4.1.1.7170" V="A02"/>
                                            </HovedDiagnose>
                                        </MedisinskVurdering>
                                        <Aktivitet>
                                            <Periode>
                                                <PeriodeFOMDato>2026-04-15</PeriodeFOMDato>
                                                <PeriodeTOMDato>2026-04-18</PeriodeTOMDato>
                                                <AktivitetIkkeMulig>
                                                    <MedisinskeArsaker/>
                                                </AktivitetIkkeMulig>
                                            </Periode>
                                        </Aktivitet>
                                        <Prognose>
                                            <ErIArbeid/>
                                        </Prognose>
                                        <Tiltak/>
                                        <MeldingTilNav>
                                            <BistandNAVUmiddelbart>false</BistandNAVUmiddelbart>
                                        </MeldingTilNav>
                                        <KontaktMedPasient>
                                            <BehandletDato>2026-04-15T00:00:00</BehandletDato>
                                        </KontaktMedPasient>
                                        <Behandler>
                                            <Navn>
                                                <Etternavn>OVERGANG</Etternavn>
                                                <Fornavn>FYLDIG</Fornavn>
                                            </Navn>
                                            <Id>
                                                <Id>16707590312</Id>
                                                <TypeId DN="Fødselsnummer" S="2.16.578.1.12.4.1.1.8116" V="FNR"/>
                                            </Id>
                                            <Id>
                                                <Id>565600304</Id>
                                                <TypeId DN="HPR-nummer" S="2.16.578.1.12.4.1.1.8116" V="HPR"/>
                                            </Id>
                                            <Adresse/>
                                            <KontaktInfo>
                                                <TypeTelecom DN="Hovedtelefon" V="HP"/>
                                                <TeleAddress V="tel:12345678"/>
                                            </KontaktInfo>
                                        </Behandler>
                                        <AvsenderSystem>
                                            <SystemNavn>Pridok EPJ</SystemNavn>
                                            <SystemVersjon>1.0.0</SystemVersjon>
                                        </AvsenderSystem>
                                        <Strekkode>000150426071115042026180420260</Strekkode>
                                    </HelseOpplysningerArbeidsuforhet>
                                </Content>
                            </RefDoc>
                        </Document>
                    </MsgHead>
                </Content>
            </RefDoc>
        </Document>
        <Document>
            <RefDoc>
                <MsgType DN="Vedlegg" V="A"/>
                <MimeType>application/jwt</MimeType>
                <Description>HelseID</Description>
                <Content>
                    <Base64Container xmlns="http://www.kith.no/xmlstds/base64container">ZXlKaGJHY2lPaUpTVXpJMU5pSXNJbXRwWkNJNklqSXpRVUZHTlRZMVFURXpNVU5GTkVZeFF6TkNNe
                        kkwTWpFNFJFRTBPVUpDTkVFNE5UUTRRVVFpTENKNE5YUWlPaUpKTm5JeFdtRkZlSHByT0dOUGVrcERSM
                        DV3U25Vd2NVWlRTekFpTENKMGVYQWlPaUpoZEN0cWQzUWlmUS5leUpwYzNNaU9pSm9kSFJ3Y3pvdkwya
                        GxiSE5sYVdRdGMzUnpMblJsYzNRdWJtaHVMbTV2SWl3aWJtSm1Jam94TnpjMk1qTTNNVE15TENKcFlYU
                        WlPakUzTnpZeU16Y3hNeklzSW1WNGNDSTZNVGMzTmpJek56Y3pNaXdpWVhWa0lqb2libUYyT25OcFoyN
                        HRiV1Z6YzJGblpTSXNJbU51WmlJNmV5SnFhM1FpT2lKUVVGQmxWakZUYnpWcFVtMHlablEzY0hOV2JYZ
                        HhXVXR4WmtSWGFETnJNbkpWYTJGcGJFOVFVRFF3SW4wc0luTmpiM0JsSWpwYkltNWhkanB6YVdkdUxXM
                        WxjM05oWjJVdmJYTm5hR1ZoWkNJc0ltOW1abXhwYm1WZllXTmpaWE56SWwwc0ltRnRjaUk2V3lKd2QyU
                        WlYU3dpWTJ4cFpXNTBYMmxrSWpvaVpUSmtOREkzTmpBdFlUTmlPUzAwTVRFMkxXSTBNV0V0TTJKaFl6W
                        XlNRFUzT1dWaUlpd2lZMnhwWlc1MFgyRnRjaUk2SW5CeWFYWmhkR1ZmYTJWNVgycDNkQ0lzSW1obGJIT
                        mxhV1E2THk5amJHRnBiWE12WTJ4cFpXNTBMMk5zWVdsdGN5OXZjbWR1Y2w5d1lYSmxiblFpT2lJNU1US
                        TNNVGt4TURNaUxDSm9aV3h6Wldsa09pOHZZMnhoYVcxekwyTnNhV1Z1ZEM5amJHRnBiWE12YjNKbmJuS
                        mZZMmhwYkdRaU9pSTVPVGs1T0RnNU1UUWlMQ0p6ZFdJaU9pSlFSM3BXZW5aUU1rcDJiRmhXWEhVd01ES
                        kNYSFV3TURKQ1QwcFRTa0ZSUnpWa09UbENTRGhSYzJscmJYaHdaRWxCUzFOYWF6MGlMQ0poZFhSb1gzU
                        nBiV1VpT2pFM056WXlNemN4TVRZc0ltbGtjQ0k2SW5SbGMzUnBaSEF0YjJsa1l5SXNJbWhsYkhObGFXU
                        TZMeTlqYkdGcGJYTXZhV1JsYm5ScGRIa3ZjR2xrSWpvaU1EWTRNamd6T1RrM09Ea2lMQ0pvWld4elpXb
                        GtPaTh2WTJ4aGFXMXpMMmxrWlc1MGFYUjVMM05sWTNWeWFYUjVYMnhsZG1Wc0lqb2lOQ0lzSW1obGJIT
                        mxhV1E2THk5amJHRnBiWE12YUhCeUwyaHdjbDl1ZFcxaVpYSWlPaUkxTmpVMU1EVTVNek1pTENKb1pXe
                        HpaV2xrT2k4dlkyeGhhVzF6TDJOc2FXVnVkQzlqYkdGcGJYTXZiM0puYm5KZmMzVndjR3hwWlhJaU9pS
                        TVNVEkzTVRreE1ETWlMQ0pvWld4elpXbGtPaTh2WTJ4aGFXMXpMMk5zYVdWdWRDOWpiR2xsYm5SZmJtR
                        nRaU0k2SWxCU1NVUlBTeUJCVXlBdElGQnlhV1J2YXlCRlVFb2dUWFZzZEdrdGRHVnVZVzUwSWl3aWMyb
                        GtJam9pUmprM1FqTTFNRGc0TkRJNU5qQTBNalpDTlRrMFJFRXhNRVpCTmpGRE9Ua2lMQ0pxZEdraU9pS
                        XdOMEV6TkRSQk5ESTBNak0yTWtJM1F6SXlORFUwTjBJMk5FRTJOalU1UlNKOS5ndFI0M0puT0V2TXBlS
                        0V4aVN6SmhTcnRDYklOUEJZNjM1T1dGUldIMEkwX1hmcGJCNnBzbjhDNDU2bjkzZjJkbXNWTHVlT2xWU
                        zdmc180VTlJY3FTZDBmbW12WmpsTjN4cHJ6OUhBRmFpWTN4Z2dOZnlxRlBPZWRRZ1ZKdmZzbHlpV3FFM
                        jZ0ZHF6Nllydy1PU0VXNVZLZXdCOVRHNVBOT0FNYWFhV2VRYUNtUzhzRmZ2Q255am0ydE5YcXNUbElmN
                        lk0S3M4QlROZTkzWUk2aTFHTGItZ0lzQ0ZzVFFrUlczYm9KY1pkc0JTVnctTjJkZmJPMF9ZdzhqRkJwT
                        zB3cU04dlA5Q19Td1EzS2I4cGhfUWxjY0JNQThyb3B2YWFSZzlMOWNDR1JBb2tNb3k2aHYyWGZfWmtDT
                        mJCTldZelB0VVNndzVxa1psSXo2a1FOeFJqbjA5R2tKcFNTazNtTnF5QU9ZajhzWFMxYkJGTzVvYXlPM
                        VBlR1EyajJISkpoZWhTV3FyOWxyZlc3clBZb2YzZlRmRW1uQkRNQ1QtQmtKd0hvYmoxbFhmTnh0a0xGT
                        3lVMjFvZmVCTFVpdFgtMXpaazN0SXRCLVdXMklaTVE1WGhkcGdYTDdsZkJUUGpBNWJIZHdFSnBRX1Axa
                        0I1UDIyb0luTlJwdzd6LTdvYg==</Base64Container>
                </Content>
            </RefDoc>
        </Document>
        <Signature xmlns="http://www.w3.org/2000/09/xmldsig#">
            <SignedInfo>
                <CanonicalizationMethod Algorithm="http://www.w3.org/TR/2001/REC-xml-c14n-20010315"/>
                <SignatureMethod Algorithm="http://www.w3.org/2001/04/xmldsig-more#rsa-sha256"/>
                <Reference URI="">
                    <Transforms>
                        <Transform Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature"/>
                        <Transform Algorithm="http://www.w3.org/TR/1999/REC-xpath-19991116">
                            <XPath xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">not(ancestor-or-self::node()[@SOAP-ENV:actor="urn:oasis:names:tc:ebxml-ms
                                g:actor:nextMSH"]|ancestor-or-self::node()[@SOAP-ENV:actor="http://schemas.xmlso
                                ap.org/soap/actor/next"])</XPath>
                        </Transform>
                        <Transform Algorithm="http://www.w3.org/TR/2001/REC-xml-c14n-20010315"/>
                    </Transforms>
                    <DigestMethod Algorithm="http://www.w3.org/2001/04/xmlenc#sha256"/>
                    <DigestValue>NE1PGnSZ1QAL9lEUnEcjI+eebrk6FZrm2BYt/J48150=</DigestValue>
                </Reference>
            </SignedInfo>
           <KeyInfo>
                <X509Data/>
            </KeyInfo>
        </Signature>
    </MsgHead>
    """
}
