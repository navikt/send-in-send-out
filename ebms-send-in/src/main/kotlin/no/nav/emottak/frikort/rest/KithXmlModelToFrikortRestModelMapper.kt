package no.nav.emottak.frikort.rest

import io.ktor.server.plugins.BadRequestException
import no.helsedir.frikort.frikorttjenester.model.CS
import no.helsedir.frikort.frikorttjenester.model.CV
import no.helsedir.frikort.frikorttjenester.model.Content
import no.helsedir.frikort.frikorttjenester.model.Document
import no.helsedir.frikort.frikorttjenester.model.EIFellesformat
import no.helsedir.frikort.frikorttjenester.model.EgenandelForesporsel
import no.helsedir.frikort.frikorttjenester.model.FrikortsporringRequest
import no.helsedir.frikort.frikorttjenester.model.HarBorgerEgenandelfritakParamType
import no.helsedir.frikort.frikorttjenester.model.HarBorgerFrikortMengdeParamType
import no.helsedir.frikort.frikorttjenester.model.HarBorgerFrikortMengdeParamTypeV2
import no.helsedir.frikort.frikorttjenester.model.HarBorgerFrikortParamType
import no.helsedir.frikort.frikorttjenester.model.HarBorgerFrikortParamTypeV2
import no.helsedir.frikort.frikorttjenester.model.HarBorgerFrikortSvar
import no.helsedir.frikort.frikorttjenester.model.HarBorgerFrikortSvarV2
import no.helsedir.frikort.frikorttjenester.model.HealthcareProfessional
import no.helsedir.frikort.frikorttjenester.model.Ident
import no.helsedir.frikort.frikorttjenester.model.MottakenhetBlokk
import no.helsedir.frikort.frikorttjenester.model.MsgHead
import no.helsedir.frikort.frikorttjenester.model.MsgInfo
import no.helsedir.frikort.frikorttjenester.model.Organization
import no.helsedir.frikort.frikorttjenester.model.Receiver
import no.helsedir.frikort.frikorttjenester.model.RefDoc
import no.helsedir.frikort.frikorttjenester.model.Sender
import no.helsedir.frikort.frikorttjenester.model.TjenestetypeKode
import no.nav.emottak.util.toKotlinxInstant
import no.nav.emottak.util.toLocalDate

fun no.trygdeetaten.xml.eiff._1.EIFellesformat.toFrikortsporringRequest() =
    FrikortsporringRequest(
        EIFellesformat(
            msgHead = this@toFrikortsporringRequest.msgHead.toMsgHead(),
            mottakenhetBlokk = this@toFrikortsporringRequest.mottakenhetBlokk.toMottakenhetBlokk()
        )
    )

private fun no.kith.xmlstds.msghead._2006_05_24.MsgHead.toMsgHead() = MsgHead(
    msgInfo = this.msgInfo.toMsgInfo(),
    documents = this@toMsgHead.document.map { it.toDocument() }
)

private fun no.kith.xmlstds.msghead._2006_05_24.Document.toDocument() = Document(
    refDoc = RefDoc(
        msgType = this@toDocument.refDoc.msgType.toCS(),
        content = this@toDocument.refDoc.content.toContent(),
        mimeType = this@toDocument.refDoc.mimeType
    ),
    documentConnection = this@toDocument.documentConnection?.toCS()
)

private fun no.kith.xmlstds.msghead._2006_05_24.RefDoc.Content.toContent(): Content {
    return when (val request = this@toContent.any.first()) {
        is no.kith.xmlstds.nav.egenandel._2010_02_01.EgenandelForesporsel -> Content(
            egenandelForesporsel = EgenandelForesporsel(
                harBorgerFrikort = request.harBorgerFrikort?.toHarBorgerFrikort(),
                harBorgerEgenandelfritak = request.harBorgerEgenandelfritak?.toHarBorgerEgenandelFritak()
            )
        )

        is no.kith.xmlstds.nav.egenandel._2016_06_10.EgenandelForesporselV2 -> Content(
            egenandelForesporselV2 = no.helsedir.frikort.frikorttjenester.model.EgenandelForesporselV2(
                harBorgerFrikort = request.harBorgerFrikort?.toHarBorgerFrikort(),
                harBorgerEgenandelfritak = request.harBorgerEgenandelfritak?.toHarBorgerEgenandelFritak()
            )
        )

        is no.kith.xmlstds.nav.egenandel._2010_02_01.EgenandelSvar -> Content(
            egenandelSvar = no.helsedir.frikort.frikorttjenester.model.EgenandelSvar(
                status = request.status.toCS(),
                svarMelding = request.svarmelding
            )
        )

        is no.kith.xmlstds.nav.egenandel._2016_06_10.EgenandelSvarV2 -> Content(
            egenandelSvarV2 = no.helsedir.frikort.frikorttjenester.model.EgenandelSvarV2(
                status = request.status.toCS(),
                svarMelding = request.svarmelding
            )
        )

        is no.kith.xmlstds.nav.egenandelmengde._2010_10_06.EgenandelMengdeForesporsel -> Content(
            egenandelMengdeForesporsel = no.helsedir.frikort.frikorttjenester.model.EgenandelMengdeForesporsel(
                harBorgerFrikort = request.harBorgerFrikort.map { it.toHarBorgerFrikortMengdeParamType() }
            )
        )

        is no.kith.xmlstds.nav.egenandelmengde._2016_06_10.EgenandelMengdeForesporselV2 -> Content(
            egenandelMengdeForesporselV2 = no.helsedir.frikort.frikorttjenester.model.EgenandelMengdeForesporselV2(
                harBorgerFrikort = request.harBorgerFrikort.map { it.toHarBorgerFrikortMengdeParamTypeV2() }
            )
        )

        is no.kith.xmlstds.nav.egenandelmengde._2010_10_06.EgenandelMengdeSvar -> Content(
            egenandelMengdeSvar = no.helsedir.frikort.frikorttjenester.model.EgenandelMengdeSvar(
                harBorgerFrikortSvar = request.harBorgerFrikortSvar.map { it.toHarBorgerFrikortSvar() }
            )
        )

        is no.kith.xmlstds.nav.egenandelmengde._2016_06_10.EgenandelMengdeSvarV2 -> Content(
            egenandelMengdeSvarV2 = no.helsedir.frikort.frikorttjenester.model.EgenandelMengdeSvarV2(
                harBorgerFrikortSvar = request.harBorgerFrikortSvar.map { it.toHarBorgerFrikortSvar() }
            )
        )
        else -> throw RuntimeException("Unknown request content: ${request.javaClass}")
    }
}

private fun no.kith.xmlstds.nav.egenandelmengde._2010_10_06.EgenandelParamType.toHarBorgerFrikortMengdeParamType() = HarBorgerFrikortMengdeParamType(
    borgerFnr = this@toHarBorgerFrikortMengdeParamType.borgerFnr,
    dato = this@toHarBorgerFrikortMengdeParamType.dato.toLocalDate()
)

private fun no.kith.xmlstds.nav.egenandelmengde._2016_06_10.EgenandelParamType.toHarBorgerFrikortMengdeParamTypeV2() =
    HarBorgerFrikortMengdeParamTypeV2(
        borgerFnr = this@toHarBorgerFrikortMengdeParamTypeV2.borgerFnr,
        dato = this@toHarBorgerFrikortMengdeParamTypeV2.dato.toLocalDate(),
        tjenestetypeKode = TjenestetypeKode.decode(this@toHarBorgerFrikortMengdeParamTypeV2.tjenestetypeKode)
            ?: throw BadRequestException("Ugyldig TjenestetypeKode: ${this@toHarBorgerFrikortMengdeParamTypeV2.tjenestetypeKode}")
    )

private fun no.kith.xmlstds.nav.egenandel._2010_02_01.EgenandelParamType.toHarBorgerFrikort() = HarBorgerFrikortParamType(
    borgerFnr = this@toHarBorgerFrikort.borgerFnr,
    dato = this@toHarBorgerFrikort.dato.toLocalDate()
)

private fun no.kith.xmlstds.nav.egenandel._2010_02_01.EgenandelParamType.toHarBorgerEgenandelFritak() = HarBorgerEgenandelfritakParamType(
    borgerFnr = this@toHarBorgerEgenandelFritak.borgerFnr,
    dato = this@toHarBorgerEgenandelFritak.dato.toLocalDate()
)

private fun no.kith.xmlstds.nav.egenandel._2016_06_10.FrikortParamType.toHarBorgerFrikort() = HarBorgerFrikortParamTypeV2(
    borgerFnr = this@toHarBorgerFrikort.borgerFnr,
    dato = this@toHarBorgerFrikort.dato.toLocalDate(),
    tjenestetypeKode = TjenestetypeKode.decode(this@toHarBorgerFrikort.tjenestetypeKode)
        ?: throw BadRequestException("Ugyldig TjenestetypeKode: ${this@toHarBorgerFrikort.tjenestetypeKode}")
)

private fun no.kith.xmlstds.nav.egenandel._2016_06_10.EgenandelfritakParamType.toHarBorgerEgenandelFritak() = HarBorgerEgenandelfritakParamType(
    borgerFnr = this@toHarBorgerEgenandelFritak.borgerFnr,
    dato = this@toHarBorgerEgenandelFritak.dato.toLocalDate()
)

private fun no.kith.xmlstds.nav.egenandelmengde._2010_10_06.EgenandelMengdeSvar.HarBorgerFrikortSvar.toHarBorgerFrikortSvar() = HarBorgerFrikortSvar(
    borgerFnr = this@toHarBorgerFrikortSvar.borgerFnr,
    dato = this@toHarBorgerFrikortSvar.dato.toLocalDate(),
    status = this@toHarBorgerFrikortSvar.status.toCS(),
    svarMelding = this@toHarBorgerFrikortSvar.svarmelding
)

private fun no.kith.xmlstds.nav.egenandelmengde._2016_06_10.EgenandelMengdeSvarV2.HarBorgerFrikortSvar.toHarBorgerFrikortSvar() = HarBorgerFrikortSvarV2(
    borgerFnr = this@toHarBorgerFrikortSvar.borgerFnr,
    dato = this@toHarBorgerFrikortSvar.dato.toLocalDate(),
    tjenestetypeKode = TjenestetypeKode.decode(this@toHarBorgerFrikortSvar.tjenestetypeKode)
        ?: throw BadRequestException("Ugyldig TjenestetypeKode: ${this@toHarBorgerFrikortSvar.tjenestetypeKode}"),
    status = this@toHarBorgerFrikortSvar.status.toCS(),
    svarMelding = this@toHarBorgerFrikortSvar.svarmelding
)

private fun no.kith.xmlstds.msghead._2006_05_24.CS.toCS() = CS(
    v = this@toCS.v,
    dn = this@toCS.dn
)

private fun no.kith.xmlstds.CS.toCS() = CS(
    v = this@toCS.v,
    dn = this@toCS.dn
)

private fun no.kith.xmlstds.msghead._2006_05_24.MsgInfo.toMsgInfo() = MsgInfo(
    type = this@toMsgInfo.type.toCS(),
    migVersion = this@toMsgInfo.miGversion,
    genDate = this@toMsgInfo.genDate.toKotlinxInstant(),
    msgId = this@toMsgInfo.msgId,
    sender = Sender(
        organization = this@toMsgInfo.sender.organisation.toOrganisation(),
        comMethod = this@toMsgInfo.sender.comMethod?.toCS()
    ),
    receiver = Receiver(
        organization = this@toMsgInfo.receiver.organisation.toOrganisation(),
        comMethod = this@toMsgInfo.receiver.comMethod?.toCS()
    )
)

private fun no.kith.xmlstds.msghead._2006_05_24.Organisation.toOrganisation() = Organization(
    organizationName = this@toOrganisation.organisationName,
    ident = this@toOrganisation.ident.map { it.toIdent() },
    healthcareProfessional = this@toOrganisation.healthcareProfessional?.toHealthcareProfessional()
)

private fun no.kith.xmlstds.msghead._2006_05_24.HealthcareProfessional.toHealthcareProfessional() =
    HealthcareProfessional(
        familyName = this@toHealthcareProfessional.familyName,
        givenName = this@toHealthcareProfessional.givenName,
        ident = this@toHealthcareProfessional.ident.map { it.toIdent() }
    )

private fun no.kith.xmlstds.msghead._2006_05_24.Ident.toIdent() = Ident(
    id = this@toIdent.id,
    typeId = CV(
        v = this@toIdent.typeId.v,
        dn = this@toIdent.typeId.dn,
        s = this@toIdent.typeId.s
    )
)

private fun no.trygdeetaten.xml.eiff._1.EIFellesformat.MottakenhetBlokk.toMottakenhetBlokk() = MottakenhetBlokk(
    ebAction = this@toMottakenhetBlokk.ebAction,
    ebService = MottakenhetBlokk.EbService.valueOf(this@toMottakenhetBlokk.ebService),
    ebRole = this@toMottakenhetBlokk.ebRole,
    ebXMLSamtaleId = this@toMottakenhetBlokk.ebXMLSamtaleId,
    ediLoggId = this@toMottakenhetBlokk.ediLoggId,
    mottaksId = this@toMottakenhetBlokk.mottaksId,
    partnerReferanse = this@toMottakenhetBlokk.partnerReferanse
)
