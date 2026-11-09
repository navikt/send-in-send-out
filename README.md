# ebms-send-in

Kotlin/Ktor-tjeneste som fungerer som **send-in**-gateway. Den mottar eBMS-meldinger fra oppstrøms tjenester ([ebms-provider](https://github.com/navikt/ebxml-processor) og [ebms-async](https://github.com/navikt/ebxml-processor)), ruter dem til riktig backend-fagsystem og returnerer svaret.

## Oppgaver

Tjenesten videresender innkommende `SendInRequest`-meldinger til ett av følgende backends basert på `service`-feltet i forespørselens adressering. Synkrone og asynkrone tjenester håndteres hver for seg, som henholdsvis `SupportedSyncServiceType` og `SupportedAsyncServiceType`.

Synkrone tjenester (`POST /fagmelding/synkron`):

| Tjeneste | Backend |
|---|---|
| `HarBorgerFrikort` | `frikorttjenester` REST API |
| `HarBorgerEgenandelFritak` | `frikorttjenester` REST API |
| `HarBorgerFrikortMengde` | `frikorttjenester` SOAP/CXF (`FrikortClient`) |
| `Inntektsforesporsel` | Utbetaling SOAP/CXF-tjeneste (`UtbetalingClient`) |

Asynkrone tjenester kan mottas enten via `POST /fagmelding/asynkron` eller ved at `EbmsInPayloadReceiver` leser `SendInRequest`-meldinger fra Kafka-topicet `team-emottak.ebms.in.payload`. Begge veier behandles av samme `FagmeldingService.processRequestAsynchronously` og sendes videre via IBM MQ:

| Tjeneste | Backend |
|---|---|
| `Trekkopplysning` | IBM MQ via `TrekkopplysningService` |
| `Sykmelding` | IBM MQ via `SyfoMeldingService` |
| `Legemelding` | IBM MQ via `LegeMeldingService` |

Alle behandlede meldinger hendelseslogges til Kafka via `EventRegistrationService`.

## Utgående meldinger via Kafka

I tillegg til svaret som returneres synkront på `POST /fagmelding/synkron`, kan svar fra fagsystemet komme asynkront tilbake som en Fellesformat-XML-melding på et Kafka-topic. Denne flyten håndteres slik:

1. `EbmsOutFellesformatReceiver` konsumerer Fellesformat-XML-meldinger fra Kafka-topicet `team-emottak.ebms.utsending.fellesformat`.
2. Meldingen unmarshalles og konverteres til en `SendInResponse` via `FagmeldingResponseService.getResponse(...)`.
3. `SendInResponse` serialiseres til JSON og produseres videre på `EbmsOutPayload`-topicet via `EbmsOutPayloadProducer`, hvor den konsumeres av nedstrøms tjenesten [ebms-async](https://github.com/navikt/ebxml-processor).

## API

| Metode | Sti | Auth | Beskrivelse |
|---|---|---|---|
| `POST` | `/fagmelding/synkron` | Azure AD | Mottar en `SendInRequest` JSON-kropp, behandler den synkront og returnerer `SendInResponse` |
| `POST` | `/fagmelding/asynkron` | Azure AD | Mottar en `SendInRequest` JSON-kropp og sender den videre asynkront (via MQ), returnerer `202 Accepted` |
| `GET` | `/internal/health/liveness` | — | Liveness-sjekk |
| `GET` | `/internal/health/readiness` | — | Readiness-sjekk |
| `GET` | `/prometheus` | — | Prometheus metrics-endepunkt |
| `GET` | `/testMq` | — | Verifiserer MQ-tilkobling for Trekkopplysning, Sykmelding og Legemelding _(kun ikke-prod)_ |

## Bygg og test

```bash
# Bygg (inkluderer formatering og lint)
./gradlew build

# Kjør kun tester
./gradlew test

# Kjør én enkelt testklasse
./gradlew test --tests "no.nav.emottak.FrikortPayloadIntegrationTest"

# Formater kode
./gradlew ktlintFormat
```

## Utrulling

GitHub Actions-workflows håndterer utrulling:

- `ebms-send-in-deploy-dev.yaml` — ruller ut til dev
- `ebms-send-in-deploy-prod.yaml` — ruller ut til prod

Docker-imaget er bygget fra en distroless Java 21-versjon.

## Avhengigheter

Eksterne tjenester denne applikasjonen er avhengig av:

- **frikorttjenester** — REST-endepunkt for frikort/egenandel-oppslag, og SOAP-endepunkt for mengde-oppslag
- **IBM MQ** — for Trekkopplysning, Sykmelding og Legemelding
- **Kafka** — for hendelseslogging, som alternativ innkanal for asynkrone `SendInRequest`-meldinger, og for utgående fagsystem-responser (Fellesformat inn, `SendInResponse` ut til ebms-async)
