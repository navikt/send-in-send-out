package no.nav.emottak.util

import no.kith.xmlstds.nav.egenandel._2010_02_01.EgenandelForesporsel
import no.kith.xmlstds.nav.egenandel._2016_06_10.EgenandelForesporselV2
import no.kith.xmlstds.nav.egenandelmengde._2010_10_06.EgenandelMengdeForesporsel
import no.kith.xmlstds.nav.egenandelmengde._2016_06_10.EgenandelMengdeForesporselV2
import no.kith.xmlstds.nav.pasientliste._2010_02_01.PasientlisteForesporsel
import no.nav.ekstern.virkemiddelokonomi.tjenester.utbetaling.v1.FinnUtbetalingListe
import no.nav.emottak.fellesformat.MessageContentMarshaller
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import org.w3c.dom.Node

const val BIRTHDAY: Int = 6
const val FNUMBER: Int = 11

fun EIFellesformat.extractReferenceParameter(): String {
    val foresporsel = this.msgHead.document.firstOrNull()?.refDoc?.content?.any?.firstOrNull().let {
        if (it is Node) {
            return@let MessageContentMarshaller.toDomainObject(it)
        }
        return@let it
    }

    return when (foresporsel) {
        is PasientlisteForesporsel -> {
            val fnrLege = foresporsel.hentPasientliste?.fnrLege
                ?: foresporsel.startAbonnement?.fnrLege
                ?: foresporsel.stoppAbonnement?.fnrLege
                ?: foresporsel.hentAbonnementStatus?.fnrLege

            fnrLege?.extractBirthDay() ?: "NA"
        }
        is FinnUtbetalingListe -> {
            foresporsel.request.bruker?.brukerId?.extractBirthDay() ?: "NA"
        }
        is EgenandelForesporselV2 -> {
            foresporsel.harBorgerEgenandelfritak?.borgerFnr?.extractBirthDay()
                ?: foresporsel.harBorgerFrikort?.borgerFnr?.extractBirthDay()
                ?: "NA"
        }
        is EgenandelForesporsel -> {
            foresporsel.harBorgerEgenandelfritak?.borgerFnr?.extractBirthDay()
                ?: foresporsel.harBorgerFrikort?.borgerFnr?.extractBirthDay()
                ?: "NA"
        }
        is EgenandelMengdeForesporselV2 -> {
            foresporsel.harBorgerFrikort.size.toString()
        }
        is EgenandelMengdeForesporsel -> {
            foresporsel.harBorgerFrikort.size.toString()
        }
        else -> "NA"
    }
}

fun String.extractBirthDay(): String {
    if (this.length != FNUMBER) {
        return "NA"
    }
    return this.substring(0, BIRTHDAY)
}
