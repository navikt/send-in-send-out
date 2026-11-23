package no.nav.emottak.util

import no.kith.xmlstds.apprec._2004_11_21.AppRec
import no.kith.xmlstds.apprec._2004_11_21.HCPerson
import no.kith.xmlstds.apprec._2004_11_21.Inst
import no.kith.xmlstds.msghead._2006_05_24.HealthcareProfessional
import no.kith.xmlstds.msghead._2006_05_24.MsgHead
import no.kith.xmlstds.msghead._2006_05_24.Organisation
import no.trygdeetaten.xml.eiff._1.EIFellesformat

internal fun EIFellesformat.toEventDataJson(): String {
    val payloadData = when {
        this.msgHead != null -> msgHead.toPayloadData()
        this.appRec != null -> appRec.toPayloadData()
        else -> PayloadData()
    }
    return encodeToJsonString(
        "partnerReferanse" to (this.mottakenhetBlokk.partnerReferanse ?: ""),
        "ediLoggId" to (this.mottakenhetBlokk.ediLoggId ?: ""),
        "msgType" to payloadData.msgType,
        "msgId" to payloadData.msgId,
        "orgNr" to payloadData.orgNr,
        "herId" to payloadData.herId,
        "behandlerHerId" to payloadData.behandlerHerId,
        "behandlerHpr" to payloadData.behandlerHpr
    )
}

private data class PayloadData(
    val msgType: String = "",
    val msgId: String = "",
    val orgNr: String = "",
    val herId: String = "",
    val behandlerHerId: String = "",
    val behandlerHpr: String = ""
)

private fun MsgHead.toPayloadData(): PayloadData {
    val receiverOrg = this.msgInfo?.receiver?.organisation
    val receiverHcp = receiverOrg?.healthcareProfessional
    return PayloadData(
        msgType = this.msgInfo?.type?.v ?: "MsgHead",
        msgId = this.msgInfo?.msgId ?: "",
        orgNr = receiverOrg.getOrgNr(),
        herId = receiverOrg.getHerId(),
        behandlerHerId = receiverHcp.getHerId(),
        behandlerHpr = receiverHcp.getHpr()
    )
}

private fun AppRec.toPayloadData(): PayloadData {
    val receiverOrg = this.receiver?.hcp?.inst
    val receiverHcp = receiverOrg?.hcPerson
    return PayloadData(
        msgType = this.msgType?.v ?: "AppRec",
        msgId = this.id ?: "",
        orgNr = receiverOrg.getOrgNr(),
        herId = receiverOrg.getHerId(),
        behandlerHerId = receiverHcp.getHerId(),
        behandlerHpr = receiverHcp.getHpr()
    )
}

private fun Organisation?.getOrgNr(): String =
    this?.ident?.firstOrNull { it.typeId?.v == "ENH" || it.typeId?.v == "orgnummer" }?.id ?: ""

private fun Organisation?.getHerId(): String =
    this?.ident?.firstOrNull { it.typeId?.v == "HER" }?.id ?: ""

private fun HealthcareProfessional?.getHerId(): String =
    this?.ident?.firstOrNull { it.typeId?.v == "HER" }?.id ?: ""

private fun HealthcareProfessional?.getHpr(): String =
    this?.ident?.firstOrNull { it.typeId?.v == "HPR" }?.id ?: ""

private fun Inst?.getOrgNr(): String =
    this?.id?.takeIf { this.typeId?.v == "ENH" || this.typeId?.v == "orgnummer" }
        ?: this?.additionalId?.firstOrNull { it.type?.v == "ENH" || it.type?.v == "orgnummer" }?.id
        ?: ""

private fun Inst?.getHerId(): String =
    this?.id?.takeIf { this.typeId?.v == "HER" }
        ?: this?.additionalId?.firstOrNull { it.type?.v == "HER" }?.id
        ?: ""

private fun List<HCPerson>?.getHerId(): String =
    this?.firstNotNullOfOrNull { person ->
        person.id?.takeIf { person.typeId?.v == "HER" }
            ?: person.additionalId.firstOrNull { it.type?.v == "HER" }?.id
    } ?: ""

private fun List<HCPerson>?.getHpr(): String =
    this?.firstNotNullOfOrNull { person ->
        person.id?.takeIf { person.typeId?.v == "HPR" }
            ?: person.additionalId.firstOrNull { it.type?.v == "HPR" }?.id
    } ?: ""
