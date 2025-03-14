package no.nav.emottak.pasientliste.validator

import no.kith.xmlstds.nav.pasientliste._2010_02_01.PasientlisteForesporsel
import no.nav.emottak.util.marker
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object PasientlisteValidator {

    private val log: Logger = LoggerFactory.getLogger(PasientlisteValidator::class.java)

    const val CONFLICT_SIGNING_SSN = "Sender FNR og legen som har signert meldingen matcher ikke."

    fun EIFellesformat.validateLegeIsAlsoSigner() {
        when (this.getLegeFnr() == this.mottakenhetBlokk.avsenderFnrFraDigSignatur) {
            true -> log.info(this.marker(), "Successfully validated that lege is also signer of the request")
            false -> {
                log.error(this.marker(), "Lege was not the signer of the request")
                throw SigningConflictException()
            }
        }
    }

    private fun EIFellesformat.getLegeFnr(): String {
        try {
            val foresporsel = this.msgHead.document.first().refDoc.content.any.first() as PasientlisteForesporsel
            return foresporsel.hentPasientliste?.fnrLege!!
        } catch (e: Exception) {
            log.error(this.marker(), "Could not find FnrLege in HentPasientliste document", e)
            throw SigningConflictException()
        }
    }

    class SigningConflictException : IllegalStateException(CONFLICT_SIGNING_SSN)
}
