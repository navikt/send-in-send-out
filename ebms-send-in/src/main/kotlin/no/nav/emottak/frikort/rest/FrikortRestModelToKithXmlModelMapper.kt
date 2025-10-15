package no.nav.emottak.frikort.rest

import no.helsedir.frikort.frikorttjenester.model.CS
import no.helsedir.frikort.frikorttjenester.model.Content
import no.helsedir.frikort.frikorttjenester.model.Document
import no.helsedir.frikort.frikorttjenester.model.HarBorgerFrikortSvar
import no.helsedir.frikort.frikorttjenester.model.HarBorgerFrikortSvarV2
import no.helsedir.frikort.frikorttjenester.model.HealthcareProfessional
import no.helsedir.frikort.frikorttjenester.model.Ident
import no.helsedir.frikort.frikorttjenester.model.MottakenhetBlokk
import no.helsedir.frikort.frikorttjenester.model.MsgHead
import no.helsedir.frikort.frikorttjenester.model.MsgInfo
import no.helsedir.frikort.frikorttjenester.model.Organization
import no.nav.emottak.util.toXmlGregorianCalendar

fun MsgHead.toMsgHead(): no.kith.xmlstds.msghead._2006_05_24.MsgHead {
    return no.kith.xmlstds.msghead._2006_05_24.MsgHead().apply {
        msgInfo = this@toMsgHead.msgInfo.toMsgInfo()
        document.addAll(this@toMsgHead.documents?.map { it.toDocument() } ?: emptyList())
    }
}

private fun MsgInfo.toMsgInfo(): no.kith.xmlstds.msghead._2006_05_24.MsgInfo {
    return no.kith.xmlstds.msghead._2006_05_24.MsgInfo().apply {
        type = this@toMsgInfo.type.toMsgHeadCS()
        miGversion = this@toMsgInfo.migVersion
        genDate = this@toMsgInfo.genDate.toXmlGregorianCalendar()
        msgId = this@toMsgInfo.msgId
        sender = no.kith.xmlstds.msghead._2006_05_24.Sender().apply {
            organisation = this@toMsgInfo.sender.organization.toOrganisation()
            comMethod = this@toMsgInfo.sender.comMethod?.toMsgHeadCS()
        }
        receiver = no.kith.xmlstds.msghead._2006_05_24.Receiver().apply {
            organisation = this@toMsgInfo.receiver.organization.toOrganisation()
            comMethod = this@toMsgInfo.receiver.comMethod?.toMsgHeadCS()
        }
    }
}

private fun Document.toDocument() = no.kith.xmlstds.msghead._2006_05_24.Document().apply {
    refDoc = no.kith.xmlstds.msghead._2006_05_24.RefDoc().apply {
        msgType = this@toDocument.refDoc.msgType.toMsgHeadCS()
        mimeType = this@toDocument.refDoc.mimeType
        content = this@toDocument.refDoc.content.toContent()
    }
    documentConnection = this@toDocument.documentConnection?.toMsgHeadCS()
}

private fun Content.toContent(): no.kith.xmlstds.msghead._2006_05_24.RefDoc.Content {
    val response = if (egenandelSvar != null) {
        no.kith.xmlstds.nav.egenandel._2010_02_01.EgenandelSvar().apply {
            status = egenandelSvar.status.toCS()
            svarmelding = egenandelSvar.svarMelding
        }
    } else if (egenandelSvarV2 != null) {
        no.kith.xmlstds.nav.egenandel._2016_06_10.EgenandelSvarV2().apply {
            status = egenandelSvarV2.status.toCS()
            svarmelding = egenandelSvarV2.svarMelding
        }
    } else if (egenandelMengdeSvar != null) {
        no.kith.xmlstds.nav.egenandelmengde._2010_10_06.EgenandelMengdeSvar().apply {
            harBorgerFrikortSvar.addAll(
                egenandelMengdeSvar.harBorgerFrikortSvar.map { it.toHarBorgerFrikortSvar() }
            )
        }
    } else if (egenandelMengdeSvarV2 != null) {
        no.kith.xmlstds.nav.egenandelmengde._2016_06_10.EgenandelMengdeSvarV2().apply {
            harBorgerFrikortSvar.addAll(
                egenandelMengdeSvarV2.harBorgerFrikortSvar.map { it.toHarBorgerFrikortSvar() }
            )
        }
    } else {
        throw RuntimeException("Unknown return content type")
    }

    return no.kith.xmlstds.msghead._2006_05_24.RefDoc.Content().apply {
        any.add(response)
    }
}

private fun HarBorgerFrikortSvar.toHarBorgerFrikortSvar() = no.kith.xmlstds.nav.egenandelmengde._2010_10_06.EgenandelMengdeSvar.HarBorgerFrikortSvar().apply {
    this.dato = this@toHarBorgerFrikortSvar.dato.toXmlGregorianCalendar()
    this.borgerFnr = this@toHarBorgerFrikortSvar.borgerFnr
    this.svarmelding = this@toHarBorgerFrikortSvar.svarMelding
    this.status = this@toHarBorgerFrikortSvar.status.toCS()
}

private fun HarBorgerFrikortSvarV2.toHarBorgerFrikortSvar() = no.kith.xmlstds.nav.egenandelmengde._2016_06_10.EgenandelMengdeSvarV2.HarBorgerFrikortSvar().apply {
    this.dato = this@toHarBorgerFrikortSvar.dato.toXmlGregorianCalendar()
    this.borgerFnr = this@toHarBorgerFrikortSvar.borgerFnr
    this.svarmelding = this@toHarBorgerFrikortSvar.svarMelding
    this.status = this@toHarBorgerFrikortSvar.status.toCS()
}

private fun CS.toMsgHeadCS() = no.kith.xmlstds.msghead._2006_05_24.CS().apply {
    v = this@toMsgHeadCS.v
    dn = this@toMsgHeadCS.dn
}

private fun CS.toCS() = no.kith.xmlstds.CS().apply {
    v = this@toCS.v
    dn = this@toCS.dn
}

private fun Organization.toOrganisation() = no.kith.xmlstds.msghead._2006_05_24.Organisation().apply {
    organisationName = this@toOrganisation.organizationName
    ident.addAll(this@toOrganisation.ident?.map { it.toIdent() } ?: emptyList())
    healthcareProfessional = this@toOrganisation.healthcareProfessional?.toHealthcareProfessional()
}

private fun HealthcareProfessional.toHealthcareProfessional(): no.kith.xmlstds.msghead._2006_05_24.HealthcareProfessional {
    return no.kith.xmlstds.msghead._2006_05_24.HealthcareProfessional().apply {
        familyName = this@toHealthcareProfessional.familyName
        givenName = this@toHealthcareProfessional.givenName
        ident.addAll(this@toHealthcareProfessional.ident?.map { it.toIdent() } ?: emptyList())
    }
}

private fun Ident.toIdent() = no.kith.xmlstds.msghead._2006_05_24.Ident().apply {
    id = this@toIdent.id
    typeId = no.kith.xmlstds.msghead._2006_05_24.CV().apply {
        v = this@toIdent.typeId.v
        dn = this@toIdent.typeId.dn
        s = this@toIdent.typeId.s
    }
}

private fun MottakenhetBlokk.toMottakenhetBlokk() = no.trygdeetaten.xml.eiff._1.EIFellesformat.MottakenhetBlokk().apply {
    ebAction = this@toMottakenhetBlokk.ebAction
    ebService = this@toMottakenhetBlokk.ebService?.value
    ebRole = this@toMottakenhetBlokk.ebRole
    ebXMLSamtaleId = this@toMottakenhetBlokk.ebXMLSamtaleId
    ediLoggId = this@toMottakenhetBlokk.ediLoggId
    mottaksId = this@toMottakenhetBlokk.mottaksId
    partnerReferanse = this@toMottakenhetBlokk.partnerReferanse
}
