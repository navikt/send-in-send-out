package no.nav.emottak.util

import no.kith.xmlstds.nav.egenandel._2010_02_01.EgenandelForesporsel
import no.kith.xmlstds.nav.egenandel._2016_06_10.EgenandelForesporselV2
import no.kith.xmlstds.nav.egenandelmengde._2010_10_06.EgenandelMengdeForesporsel
import no.kith.xmlstds.nav.egenandelmengde._2016_06_10.EgenandelMengdeForesporselV2
import no.kith.xmlstds.nav.pasientliste._2010_02_01.PasientlisteForesporsel
import no.nav.ekstern.virkemiddelokonomi.tjenester.utbetaling.v1.FinnUtbetalingListe
import no.trygdeetaten.xml.eiff._1.EIFellesformat

const val BIRTHDAY: Int = 6
const val FNUMBER: Int = 11

fun EIFellesformat.extractReferenceParameter(): String {
    val foresporsel = this.msgHead.document.firstOrNull()?.refDoc?.content?.any?.firstOrNull()

    return when (foresporsel) {
        is PasientlisteForesporsel -> {
            foresporsel.hentPasientliste?.fnrLege?.extractBirthDay() ?: "NA"
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
