package no.nav.emottak.util

import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import no.kith.xmlstds.nav.egenandel._2010_02_01.EgenandelForesporsel
import no.kith.xmlstds.nav.egenandel._2016_06_10.EgenandelForesporselV2
import no.kith.xmlstds.nav.egenandelmengde._2010_10_06.EgenandelMengdeForesporsel
import no.kith.xmlstds.nav.egenandelmengde._2016_06_10.EgenandelMengdeForesporselV2
import no.nav.ekstern.virkemiddelokonomi.tjenester.utbetaling.v1.FinnUtbetalingListe
import no.nav.emottak.fellesformat.MessageContentMarshaller
import no.trygdeetaten.xml.eiff._1.EIFellesformat
import org.w3c.dom.Node

const val BIRTHDAY: Int = 4
const val FNUMBER: Int = 11

fun EIFellesformat.extractReferenceParameter(): String {
    val foresporsel = when (val content = this.msgHead.document.firstOrNull()?.refDoc?.content?.any?.firstOrNull()) {
        is Node ->
            if (content.localName in setOf(
                    "finnUtbetalingListe",
                    "EgenandelForesporsel",
                    "EgenandelForesporselV2",
                    "EgenandelMengdeForesporsel",
                    "EgenandelMengdeForesporselV2",
                    "MsgHead"
                )
            ) {
                MessageContentMarshaller.toDomainObject(content)
            } else {
                null
            }
        else -> content
    }

    return when (foresporsel) {
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
        is MsgHead -> {
            foresporsel.msgInfo.patient?.ident
                ?.firstOrNull { it.typeId.v == "FNR" }?.id?.extractBirthDay()
                ?: "NA"
        }
        else -> {
            this.msgHead.msgInfo?.patient?.ident
                ?.firstOrNull { it.typeId.v == "FNR" }?.id?.extractBirthDay()
                ?: this.msgHead.patientReport.firstOrNull()?.patient?.ident
                    ?.firstOrNull { it.typeId.v == "FNR" }?.id?.extractBirthDay()
                ?: "NA"
        }
    }
}

fun String.extractBirthDay(): String {
    if (this.length != FNUMBER) {
        return "NA"
    }
    return this.substring(0, BIRTHDAY)
}
