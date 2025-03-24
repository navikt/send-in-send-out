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

fun refParamFrikort(foresporsel: Any): String {
    return when (foresporsel) {
        is EgenandelForesporselV2 -> {
            foresporsel.harBorgerEgenandelfritak?.borgerFnr ?: foresporsel.harBorgerFrikort?.borgerFnr ?: "NA"
        }
        is EgenandelForesporsel -> {
            foresporsel.harBorgerEgenandelfritak?.borgerFnr ?: foresporsel.harBorgerFrikort?.borgerFnr ?: "NA"
        }
        is EgenandelMengdeForesporselV2 -> {
            foresporsel.harBorgerFrikort?.size.toString() ?: "NA"
        }
        is EgenandelMengdeForesporsel -> {
            foresporsel.harBorgerFrikort?.size.toString() ?: "NA"
        }
        else -> "NA"
    }
}

fun refParam(fellesformat: EIFellesformat): String {
    val foresporsel = fellesformat.msgHead.document.first().refDoc.content.any.first()

    return when (foresporsel) {
        is PasientlisteForesporsel -> {
            foresporsel.hentPasientliste?.fnrLege ?: "NA"
        }
        is FinnUtbetalingListe -> {
            foresporsel.request.bruker?.brukerId ?: "NA"
        }
        else -> "NA"
    }
}

fun birthDay(fnr: String): String {
    if (fnr.length != FNUMBER) {
        return "NA"
    }
    return fnr.substring(0, BIRTHDAY)
}
