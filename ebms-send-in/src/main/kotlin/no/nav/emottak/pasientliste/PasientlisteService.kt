package no.nav.emottak.pasientliste

import no.nav.emottak.pasientliste.validator.PasientlisteValidator.validateLegeIsAlsoSigner
import no.nav.emottak.util.LogLevel
import no.nav.emottak.util.asXml
import no.nav.emottak.util.marker
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object PasientlisteService {

    private val log: Logger = LoggerFactory.getLogger(PasientlisteService::class.java)

    fun pasientlisteForesporsel(fellesformatRequest: EIFellesformat): EIFellesformat =
        when (fellesformatRequest.mottakenhetBlokk.ebAction) {
            "HentPasientliste", "StartAbonnement", "StoppAbonnement", "HentAbonnementStatus" -> {
                fellesformatRequest.validateLegeIsAlsoSigner()
                forwardRequest(fellesformatRequest)
            }
            else -> throw NotImplementedError(
                "Action: ${fellesformatRequest.mottakenhetBlokk.ebAction} for service: " +
                    "${fellesformatRequest.mottakenhetBlokk.ebService} is not implemented"
            )
        }

    private fun forwardRequest(fellesformatRequest: EIFellesformat) =
        PasientlisteClient.sendRequest(fellesformatRequest).also {
            log.asXml(LogLevel.DEBUG, "Response from PasientlisteClient", it, it.marker())
        }
}
