package no.nav.emottak.pasientliste.validator

import no.kith.xmlstds.nav.pasientliste._2010_02_01.PasientlisteForesporsel
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object PasientlisteValidator {

    private val log: Logger = LoggerFactory.getLogger(PasientlisteValidator::class.java)

    const val CONFLICT_SIGNING_SSN =
        "Sender FNR og legen som har signert meldingen skall ikke vare forskjelige."

    fun validateLegeIsAlsoSigner(
        fellesformatRequest: EIFellesformat,
        signedOf: String
    ) {
        log.debug("Validating that the request is signed with the same SSN as the doctor (lege)")
        val fnrLege = getLegeFnr(fellesformatRequest)
        if (fnrLege != signedOf) {
            log.error("Lege was not the signer of the request")
            throw SigningConflictException()
        }
        log.debug("Successfully validated that lege is also signer of the request")
    }

    private fun getLegeFnr(fellesformatRequest: EIFellesformat): String {
        try {
            val foresporsel =
                fellesformatRequest.msgHead.document.first().refDoc.content.any.first() as PasientlisteForesporsel
            return foresporsel.hentPasientliste?.fnrLege!!
        } catch (e: Exception) {
            log.error("Could not find FnrLege in HentPasientliste document", e)
            throw SigningConflictException()
        }
    }

    class SigningConflictException : IllegalStateException(CONFLICT_SIGNING_SSN)
}
