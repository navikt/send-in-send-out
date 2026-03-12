# ebms-send-in

Kotlin/Ktor-tjeneste som fungerer som **send-in**-gateway i `emottak`-plattformen. Den mottar eBMS-meldinger fra oppstrøms tjenester ([ebms-provider](https://github.com/navikt/ebxml-processor) og [ebms-async](https://github.com/navikt/ebxml-processor)), ruter dem til riktig backend-fagsystem og returnerer svaret.

## Ansvar

Tjenesten videresender innkommende `SendInRequest`-meldinger til ett av følgende backends basert på `service`-feltet i forespørselens adressering:

| Tjeneste | Backend |
|---|---|
| `HarBorgerFrikort` | `frikorttjenester` REST API (eller legacy SOAP for `cpaId = nav:70079`) |
| `HarBorgerEgenandelFritak` | `frikorttjenester` REST API (eller legacy SOAP for `cpaId = nav:70079`) |
| `HarBorgerFrikortMengde` | `frikorttjenester` SOAP/CXF |
| `Inntektsforesporsel` | Utbetaling SOAP/CXF-tjeneste |
| `Trekkopplysning` | IBM MQ |

Alle asynkrone behandlede meldinger hendelseslogges til Kafka via `EventRegistrationService`.

## API

| Metode | Sti | Auth | Beskrivelse |
|---|---|---|---|
| `POST` | `/fagmelding/synkron` | Azure AD | Hovedinngangspunkt — mottar en `SendInRequest` JSON-kropp og returnerer `SendInResponse` |
| `GET` | `/internal/health/liveness` | — | Liveness-sjekk |
| `GET` | `/internal/health/readiness` | — | Readiness-sjekk |
| `GET` | `/prometheus` | — | Prometheus metrics-endepunkt |
| `GET` | `/testMq` | — | Verifiserer IBM MQ-tilkobling _(kun ikke-prod)_ |

## Bygg og test

```bash
# Bygg (inkluderer formatering og lint)
./gradlew build

# Kjør kun tester
./gradlew test

# Formater kode
./gradlew ktlintFormat
```

## Utrulling

GitHub Actions-workflows håndterer utrulling:

- `ebms-send-in-deploy-dev.yaml` — ruller ut til dev
- `ebms-send-in-deploy-prod.yaml` — ruller ut til prod

Docker-imaget er bygget fra en distroless Java 21-base og eksponerer `build/libs/app.jar`.

## Avhengigheter

Eksterne tjenester denne applikasjonen er avhengig av:

- **frikorttjenester** — REST- og SOAP-endepunkter for frikort/egenandel-oppslag
- **IBM MQ** — for Trekkopplysning
- **Kafka** — for hendelseslogging
- **Azure AD** — for tokenvalidering på innkommende forespørsler og tokenanskaffelse for utgående kall
