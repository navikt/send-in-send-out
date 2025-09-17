package no.nav.emottak.melding.model

import kotlinx.serialization.Serializable
import no.kith.xmlstds.msghead._2006_05_24.Organisation
import no.kith.xmlstds.nav.egenandel._2010_02_01.EgenandelParamType
import no.kith.xmlstds.nav.egenandel._2016_06_10.EgenandelfritakParamType
import no.kith.xmlstds.nav.egenandel._2016_06_10.FrikortParamType
import no.nav.emottak.melding.model.CS.Companion.fromKithCS
import no.nav.emottak.melding.model.CS.Companion.toKithCS
import no.nav.emottak.melding.model.CS.Companion.toKithMsgHeadCS
import no.nav.emottak.melding.model.CV.Companion.fromKithCV
import no.nav.emottak.melding.model.CV.Companion.toKithCV
import no.nav.emottak.melding.model.Content.Companion.fromKithContent
import no.nav.emottak.melding.model.Content.Companion.toKithContent
import no.nav.emottak.melding.model.ConversationRef.Companion.fromKithConversationRef
import no.nav.emottak.melding.model.ConversationRef.Companion.toKithConversationRef
import no.nav.emottak.melding.model.Document.Companion.fromKithDocument
import no.nav.emottak.melding.model.Document.Companion.toKithDocument
import no.nav.emottak.melding.model.EIFellesformat.Companion.fromNavFellesformat
import no.nav.emottak.melding.model.EgenandelForesporsel.Companion.fromKithEgenandelForesporsel
import no.nav.emottak.melding.model.EgenandelForesporsel.Companion.toKithEgenandelForesporsel
import no.nav.emottak.melding.model.EgenandelForesporselV2.Companion.fromKithEgenandelForesporselV2
import no.nav.emottak.melding.model.EgenandelForesporselV2.Companion.toKithEgenandelForesporselV2
import no.nav.emottak.melding.model.EgenandelSvar.Companion.fromKithEgenandelSvar
import no.nav.emottak.melding.model.EgenandelSvar.Companion.toKithEgenandelSvar
import no.nav.emottak.melding.model.EgenandelSvarV2.Companion.fromKithEgenandelSvarV2
import no.nav.emottak.melding.model.EgenandelSvarV2.Companion.toKithEgenandelSvarV2
import no.nav.emottak.melding.model.HarBorgerEgenandelfritakParamType.Companion.toFrikortHarBorgerEgenandelfritakParamType
import no.nav.emottak.melding.model.HarBorgerEgenandelfritakParamType.Companion.toKithEgenandelParamType
import no.nav.emottak.melding.model.HarBorgerEgenandelfritakParamType.Companion.toKithEgenandelfritakParamType
import no.nav.emottak.melding.model.HarBorgerFrikortParamType.Companion.toFrikortHarBorgerFrikortParamType
import no.nav.emottak.melding.model.HarBorgerFrikortParamType.Companion.toKithEgenandelParamType
import no.nav.emottak.melding.model.HarBorgerFrikortParamTypeV2.Companion.toFrikortHarBorgerFrikortParamTypeV2
import no.nav.emottak.melding.model.HarBorgerFrikortParamTypeV2.Companion.toKithFrikortParamType
import no.nav.emottak.melding.model.HealthcareProfessional.Companion.fromKithHealthcareProfessional
import no.nav.emottak.melding.model.HealthcareProfessional.Companion.toKithHealthcareProfessional
import no.nav.emottak.melding.model.Ident.Companion.fromKithIdent
import no.nav.emottak.melding.model.Ident.Companion.toKithIdent
import no.nav.emottak.melding.model.MottakenhetBlokk.Companion.fromNavMottakenhetBlokk
import no.nav.emottak.melding.model.MsgHead.Companion.fromKithMsgHead
import no.nav.emottak.melding.model.MsgInfo.Companion.fromKithMsgInfo
import no.nav.emottak.melding.model.MsgInfo.Companion.toKithMsgInfo
import no.nav.emottak.melding.model.Organization.Companion.fromKithOrganisation
import no.nav.emottak.melding.model.Organization.Companion.toKithOrganisation
import no.nav.emottak.melding.model.Receiver.Companion.fromKithReceiver
import no.nav.emottak.melding.model.Receiver.Companion.toKithReceiver
import no.nav.emottak.melding.model.RefDoc.Companion.fromKithRefDoc
import no.nav.emottak.melding.model.RefDoc.Companion.toKithRefDoc
import no.nav.emottak.melding.model.Sender.Companion.fromKithSender
import no.nav.emottak.melding.model.Sender.Companion.toKithSender
import no.nav.emottak.melding.model.TjenestetypeKode.Companion.toKithTjenestetypeKode
import no.nav.emottak.melding.model.TjenestetypeKode.Companion.toTjenestetypeKode
import no.nav.emottak.util.toXmlGregorianCalendar
import kotlin.String

@Serializable
data class FrikortsporringRequest(
    val eiFellesformat: EIFellesformat
) {
    companion object {
        fun no.trygdeetaten.xml.eiff._1.EIFellesformat.asFrikortsporringRequest() = FrikortsporringRequest(
            eiFellesformat = this.fromNavFellesformat()
        )
    }
}

@Serializable
data class EIFellesformat(
    val msgHead: MsgHead,
    val mottakenhetBlokk: MottakenhetBlokk
) {
    companion object {
        fun no.trygdeetaten.xml.eiff._1.EIFellesformat.fromNavFellesformat() = EIFellesformat(
            msgHead = this.msgHead.fromKithMsgHead(),
            mottakenhetBlokk = this.mottakenhetBlokk.fromNavMottakenhetBlokk()
        )
    }
}

@Serializable
data class MsgHead(
    val msgInfo: MsgInfo,
    val documents: List<Document>? = null
) {
    companion object {
        fun MsgHead.toKithMsgHead() = no.kith.xmlstds.msghead._2006_05_24.MsgHead().apply {
            msgInfo = this@toKithMsgHead.msgInfo.toKithMsgInfo()
            this@toKithMsgHead.documents?.forEach { document.add(it.toKithDocument()) }
        }

        fun no.kith.xmlstds.msghead._2006_05_24.MsgHead.fromKithMsgHead() = MsgHead(
            msgInfo = this.msgInfo.fromKithMsgInfo(),
            documents = this.document.map { it.fromKithDocument() }
        )
    }
}

@Serializable
data class MottakenhetBlokk(
    val ebAction: String,
    val ebService: String,
    val ebRole: String,
    val ebXMLSamtaleId: String,
    val ediLoggId: String? = null,
    val mottaksId: String? = null,
    val partnerReferanse: String? = null
) {
    companion object {
        fun no.trygdeetaten.xml.eiff._1.EIFellesformat.MottakenhetBlokk.fromNavMottakenhetBlokk() = MottakenhetBlokk(
            ebAction = this.ebAction,
            ebService = this.ebService,
            ebRole = this.ebRole,
            ebXMLSamtaleId = this.ebXMLSamtaleId,
            ediLoggId = this.ediLoggId,
            mottaksId = this.mottaksId,
            partnerReferanse = this.partnerReferanse
        )
    }
}

@Serializable
data class MsgInfo(
    val type: CS,
    val migVersion: String,
    val ack: CS? = null,
    val conversationRef: ConversationRef? = null,
    val genDate: String,
    val msgId: String,
    val sender: Sender,
    val receiver: Receiver
) {
    companion object {
        fun MsgInfo.toKithMsgInfo() = no.kith.xmlstds.msghead._2006_05_24.MsgInfo().apply {
            type = this@toKithMsgInfo.type.toKithMsgHeadCS()
            miGversion = this@toKithMsgInfo.migVersion
            ack = this@toKithMsgInfo.ack?.toKithMsgHeadCS()
            conversationRef = this@toKithMsgInfo.conversationRef?.toKithConversationRef()
            genDate = this@toKithMsgInfo.genDate.toXmlGregorianCalendar()
            msgId = this@toKithMsgInfo.msgId
            sender = this@toKithMsgInfo.sender.toKithSender()
            receiver = this@toKithMsgInfo.receiver.toKithReceiver()
        }

        fun no.kith.xmlstds.msghead._2006_05_24.MsgInfo.fromKithMsgInfo() = MsgInfo(
            type = this.type.fromKithCS(),
            migVersion = this.miGversion,
            ack = this.ack?.fromKithCS(),
            conversationRef = this.conversationRef?.fromKithConversationRef(),
            genDate = this.genDate.toString(),
            msgId = this.msgId,
            sender = this.sender.fromKithSender(),
            receiver = this.receiver.fromKithReceiver()
        )
    }
}

@Serializable
data class ConversationRef(
    val refToParent: String,
    val refToConversation: String
) {
    companion object {
        fun ConversationRef.toKithConversationRef() = no.kith.xmlstds.msghead._2006_05_24.ConversationRef().apply {
            refToParent = this@toKithConversationRef.refToParent
            refToConversation = this@toKithConversationRef.refToConversation
        }
        fun no.kith.xmlstds.msghead._2006_05_24.ConversationRef.fromKithConversationRef() = ConversationRef(
            refToParent = this.refToParent,
            refToConversation = this.refToConversation
        )
    }
}

@Serializable
data class CS(
    val v: String? = null,
    val dn: String? = null
) {
    companion object {
        fun CS.toKithMsgHeadCS() = no.kith.xmlstds.msghead._2006_05_24.CS().apply {
            v = this@toKithMsgHeadCS.v
            dn = this@toKithMsgHeadCS.dn
        }

        fun CS.toKithCS() = no.kith.xmlstds.CS().apply {
            v = this@toKithCS.v
            dn = this@toKithCS.dn
        }

        fun no.kith.xmlstds.msghead._2006_05_24.CS.fromKithCS() = CS(
            v = this.v,
            dn = this.dn
        )

        fun no.kith.xmlstds.CS.fromKithCS() = CS(
            v = this.v,
            dn = this.dn
        )
    }
}

@Serializable
data class Sender(
    val organization: Organization,
    val comMethod: CS? = null
) {
    companion object {
        fun Sender.toKithSender() = no.kith.xmlstds.msghead._2006_05_24.Sender().apply {
            organisation = this@toKithSender.organization.toKithOrganisation()
            comMethod = this@toKithSender.comMethod?.toKithMsgHeadCS()
        }
        fun no.kith.xmlstds.msghead._2006_05_24.Sender.fromKithSender() = Sender(
            organization = this.organisation.fromKithOrganisation(),
            comMethod = this.comMethod?.fromKithCS()
        )
    }
}

@Serializable
data class Receiver(
    val organization: Organization,
    val comMethod: CS? = null
) {
    companion object {
        fun Receiver.toKithReceiver() = no.kith.xmlstds.msghead._2006_05_24.Receiver().apply {
            organisation = this@toKithReceiver.organization.toKithOrganisation()
            comMethod = this@toKithReceiver.comMethod?.toKithMsgHeadCS()
        }
        fun no.kith.xmlstds.msghead._2006_05_24.Receiver.fromKithReceiver() = Receiver(
            organization = this.organisation.fromKithOrganisation(),
            comMethod = this.comMethod?.fromKithCS()
        )
    }
}

@Serializable
data class Organization(
    val organizationName: String? = null,
    val ident: List<Ident>? = null,
    val healthcareProfessional: HealthcareProfessional? = null
) {
    companion object {
        fun Organization.toKithOrganisation() = Organisation().apply {
            organisationName = this@toKithOrganisation.organizationName
            this@toKithOrganisation.ident?.forEach { ident.add(it.toKithIdent()) }
            healthcareProfessional = this@toKithOrganisation.healthcareProfessional?.toKithHealthcareProfessional()
        }
        fun Organisation.fromKithOrganisation() = Organization(
            organizationName = this.organisationName,
            ident = this.ident.map { it.fromKithIdent() },
            healthcareProfessional = this.healthcareProfessional?.fromKithHealthcareProfessional()
        )
    }
}

@Serializable
data class Ident(
    val id: String,
    val typeId: CV
) {
    companion object {
        fun Ident.toKithIdent() = no.kith.xmlstds.msghead._2006_05_24.Ident().apply {
            id = this@toKithIdent.id
            typeId = this@toKithIdent.typeId.toKithCV()
        }
        fun no.kith.xmlstds.msghead._2006_05_24.Ident.fromKithIdent() = Ident(
            id = this.id,
            typeId = this.typeId.fromKithCV()
        )
    }
}

@Serializable
data class HealthcareProfessional(
    val familyName: String? = null,
    val givenName: String? = null,
    val ident: List<Ident>? = null
) {
    companion object {
        fun HealthcareProfessional.toKithHealthcareProfessional() = no.kith.xmlstds.msghead._2006_05_24.HealthcareProfessional().apply {
            familyName = this@toKithHealthcareProfessional.familyName
            givenName = this@toKithHealthcareProfessional.givenName
            this@toKithHealthcareProfessional.ident?.forEach { ident.add(it.toKithIdent()) }
        }
        fun no.kith.xmlstds.msghead._2006_05_24.HealthcareProfessional.fromKithHealthcareProfessional() =
            HealthcareProfessional(
                familyName = this.familyName,
                givenName = this.givenName,
                ident = this.ident.map { it.fromKithIdent() }
            )
    }
}

@Serializable
data class CV(
    val v: String? = null,
    val dn: String? = null,
    val s: String? = null
) {
    companion object {
        fun CV.toKithCV() = no.kith.xmlstds.msghead._2006_05_24.CV().apply {
            v = this@toKithCV.v
            dn = this@toKithCV.dn
            s = this@toKithCV.s
        }
        fun no.kith.xmlstds.msghead._2006_05_24.CV.fromKithCV() = CV(
            v = this.v,
            dn = this.dn,
            s = this.s
        )
    }
}

@Serializable
data class Document(
    val documentConnection: CS? = null,
    val refDoc: RefDoc
) {
    companion object {
        fun Document.toKithDocument() = no.kith.xmlstds.msghead._2006_05_24.Document().apply {
            documentConnection = this@toKithDocument.documentConnection?.toKithMsgHeadCS()
            refDoc = this@toKithDocument.refDoc.toKithRefDoc()
        }

        fun no.kith.xmlstds.msghead._2006_05_24.Document.fromKithDocument() = Document(
            documentConnection = this.documentConnection.fromKithCS(),
            refDoc = this.refDoc.fromKithRefDoc()
        )
    }
}

@Serializable
data class RefDoc(
    val msgType: CS,
    val content: Content? = null,
    val mimeType: String? = null
) {
    companion object {
        fun RefDoc.toKithRefDoc() = no.kith.xmlstds.msghead._2006_05_24.RefDoc().apply {
            msgType = this@toKithRefDoc.msgType.toKithMsgHeadCS()
            content = this@toKithRefDoc.content?.toKithContent()
            mimeType = this@toKithRefDoc.mimeType
        }

        fun no.kith.xmlstds.msghead._2006_05_24.RefDoc.fromKithRefDoc() = RefDoc(
            msgType = this.msgType.fromKithCS(),
            content = this.content.fromKithContent(),
            mimeType = this.mimeType
        )
    }
}

@Serializable
data class Content(
    val egenandelForesporsel: EgenandelForesporsel? = null,
    val egenandelForesporselV2: EgenandelForesporselV2? = null,
    val egenandelSvar: EgenandelSvar? = null,
    val egenandelSvarV2: EgenandelSvarV2? = null,
    val egenandelMengdeForesporsel: EgenandelMengdeForesporsel? = null,
    val egenandelMengdeForesporselV2: EgenandelMengdeForesporselV2? = null,
    val egenandelMengdeSvar: EgenandelMengdeSvar? = null,
    val egenandelMengdeSvarV2: EgenandelMengdeSvarV2? = null
) {
    companion object {
        fun Content.toKithContent(): no.kith.xmlstds.msghead._2006_05_24.RefDoc.Content {
            val responseContent = when {
                (egenandelForesporsel != null) -> egenandelForesporsel.toKithEgenandelForesporsel()
                (egenandelForesporselV2 != null) -> egenandelForesporselV2.toKithEgenandelForesporselV2()
                (egenandelSvar != null) -> egenandelSvar.toKithEgenandelSvar()
                (egenandelSvarV2 != null) -> egenandelSvarV2.toKithEgenandelSvarV2()
                else -> throw RuntimeException("Unknown message type")
            }
            return no.kith.xmlstds.msghead._2006_05_24.RefDoc.Content().apply {
                this.any.add(responseContent)
            }
        }

        fun no.kith.xmlstds.msghead._2006_05_24.RefDoc.Content.fromKithContent(): Content {
            return when (val contentType = this.any.first()) {
                is no.kith.xmlstds.nav.egenandel._2010_02_01.EgenandelForesporsel -> Content(egenandelForesporsel = contentType.fromKithEgenandelForesporsel())
                is no.kith.xmlstds.nav.egenandel._2016_06_10.EgenandelForesporselV2 -> Content(egenandelForesporselV2 = contentType.fromKithEgenandelForesporselV2())
                is no.kith.xmlstds.nav.egenandel._2010_02_01.EgenandelSvar -> Content(egenandelSvar = contentType.fromKithEgenandelSvar())
                is no.kith.xmlstds.nav.egenandel._2016_06_10.EgenandelSvarV2 -> Content(egenandelSvarV2 = contentType.fromKithEgenandelSvarV2())
                else -> throw RuntimeException("Unknown message type")
            }
        }
    }
}

@Serializable
data class EgenandelForesporsel(
    val harBorgerFrikort: HarBorgerFrikortParamType? = null,
    val harBorgerEgenandelfritak: HarBorgerEgenandelfritakParamType? = null
) {
    companion object {
        fun EgenandelForesporsel.toKithEgenandelForesporsel() = no.kith.xmlstds.nav.egenandel._2010_02_01.EgenandelForesporsel().apply {
            harBorgerFrikort = this@toKithEgenandelForesporsel.harBorgerFrikort?.toKithEgenandelParamType()
            harBorgerEgenandelfritak = this@toKithEgenandelForesporsel.harBorgerEgenandelfritak?.toKithEgenandelParamType()
        }

        fun no.kith.xmlstds.nav.egenandel._2010_02_01.EgenandelForesporsel.fromKithEgenandelForesporsel() = EgenandelForesporsel(
            harBorgerFrikort = this.harBorgerFrikort?.toFrikortHarBorgerFrikortParamType(),
            harBorgerEgenandelfritak = this.harBorgerEgenandelfritak?.toFrikortHarBorgerEgenandelfritakParamType()
        )
    }
}

@Serializable
data class EgenandelForesporselV2(
    val harBorgerFrikort: HarBorgerFrikortParamTypeV2? = null,
    val harBorgerEgenandelfritak: HarBorgerEgenandelfritakParamType? = null
) {
    companion object {
        fun EgenandelForesporselV2.toKithEgenandelForesporselV2() = no.kith.xmlstds.nav.egenandel._2016_06_10.EgenandelForesporselV2().apply {
            harBorgerFrikort = this@toKithEgenandelForesporselV2.harBorgerFrikort?.toKithFrikortParamType()
            harBorgerEgenandelfritak = this@toKithEgenandelForesporselV2.harBorgerEgenandelfritak?.toKithEgenandelfritakParamType()
        }

        fun no.kith.xmlstds.nav.egenandel._2016_06_10.EgenandelForesporselV2.fromKithEgenandelForesporselV2() = EgenandelForesporselV2(
            harBorgerFrikort = this.harBorgerFrikort?.toFrikortHarBorgerFrikortParamTypeV2(),
            harBorgerEgenandelfritak = this.harBorgerEgenandelfritak?.toFrikortHarBorgerEgenandelfritakParamType()
        )
    }
}

@Serializable
data class EgenandelMengdeForesporsel(
    val harBorgerFrikort: List<HarBorgerFrikortMengdeParamType>? = null
)

@Serializable
data class EgenandelMengdeForesporselV2(
    val harBorgerFrikort: List<HarBorgerFrikortMengdeParamTypeV2>? = null
)

@Serializable
data class HarBorgerFrikortParamType(
    val borgerFnr: String,
    val dato: String
) {
    companion object {
        fun HarBorgerFrikortParamType.toKithEgenandelParamType() = EgenandelParamType().apply {
            borgerFnr = this@toKithEgenandelParamType.borgerFnr
            dato = this@toKithEgenandelParamType.dato.toXmlGregorianCalendar()
        }

        fun EgenandelParamType.toFrikortHarBorgerFrikortParamType() = HarBorgerFrikortParamType(
            borgerFnr = this.borgerFnr,
            dato = this.dato.toString()
        )
    }
}

@Serializable
data class HarBorgerFrikortParamTypeV2(
    val borgerFnr: String,
    val dato: String,
    val tjenestetypeKode: TjenestetypeKode
) {
    companion object {
        fun HarBorgerFrikortParamTypeV2.toKithFrikortParamType() = FrikortParamType().apply {
            borgerFnr = this@toKithFrikortParamType.borgerFnr
            dato = this@toKithFrikortParamType.dato.toXmlGregorianCalendar()
            tjenestetypeKode = this@toKithFrikortParamType.tjenestetypeKode.toKithTjenestetypeKode()
        }

        fun FrikortParamType.toFrikortHarBorgerFrikortParamTypeV2() = HarBorgerFrikortParamTypeV2(
            borgerFnr = this.borgerFnr,
            dato = this.dato.toString(),
            tjenestetypeKode = this.tjenestetypeKode.toTjenestetypeKode()
        )
    }
}

@Serializable
data class HarBorgerEgenandelfritakParamType(
    val borgerFnr: String,
    val dato: String
) {
    companion object {
        fun HarBorgerEgenandelfritakParamType.toKithEgenandelfritakParamType() = EgenandelfritakParamType().apply {
            borgerFnr = this@toKithEgenandelfritakParamType.borgerFnr
            dato = this@toKithEgenandelfritakParamType.dato.toXmlGregorianCalendar()
        }

        fun HarBorgerEgenandelfritakParamType.toKithEgenandelParamType() = EgenandelParamType().apply {
            borgerFnr = this@toKithEgenandelParamType.borgerFnr
            dato = this@toKithEgenandelParamType.dato.toXmlGregorianCalendar()
        }

        fun EgenandelParamType.toFrikortHarBorgerEgenandelfritakParamType() = HarBorgerEgenandelfritakParamType(
            borgerFnr = this.borgerFnr,
            dato = this.dato.toString()
        )

        fun EgenandelfritakParamType.toFrikortHarBorgerEgenandelfritakParamType() = HarBorgerEgenandelfritakParamType(
            borgerFnr = this.borgerFnr,
            dato = this.dato.toString()
        )
    }
}

@Serializable
data class EgenandelSvar(
    val status: CS,
    val svarMelding: String
) {
    companion object {
        fun EgenandelSvar.toKithEgenandelSvar() = no.kith.xmlstds.nav.egenandel._2010_02_01.EgenandelSvar().apply {
            status = this@toKithEgenandelSvar.status.toKithCS()
            svarmelding = this@toKithEgenandelSvar.svarMelding
        }
        fun no.kith.xmlstds.nav.egenandel._2010_02_01.EgenandelSvar.fromKithEgenandelSvar() = EgenandelSvar(
            status = this.status.fromKithCS(),
            svarMelding = this.svarmelding
        )
    }
}

@Serializable
data class EgenandelSvarV2(
    val status: CS,
    val svarMelding: String
) {
    companion object {
        fun EgenandelSvarV2.toKithEgenandelSvarV2() = no.kith.xmlstds.nav.egenandel._2016_06_10.EgenandelSvarV2().apply {
            status = this@toKithEgenandelSvarV2.status.toKithCS()
            svarmelding = this@toKithEgenandelSvarV2.svarMelding
        }
        fun no.kith.xmlstds.nav.egenandel._2016_06_10.EgenandelSvarV2.fromKithEgenandelSvarV2() = EgenandelSvarV2(
            status = this.status.fromKithCS(),
            svarMelding = this.svarmelding
        )
    }
}

@Serializable
data class EgenandelMengdeSvar(
    val harBorgerFrikortSvar: List<HarBorgerFrikortSvar>
)

@Serializable
data class EgenandelMengdeSvarV2(
    val harBorgerFrikortSvar: List<HarBorgerFrikortSvarV2>
)

@Serializable
data class FrikortsporringResponse(
    val eiFellesformat: EIFellesformat
)

@Serializable
data class HarBorgerFrikortMengdeParamType(
    val borgerFnr: String,
    val dato: String
)

@Serializable
data class HarBorgerFrikortMengdeParamTypeV2(
    val borgerFnr: String,
    val dato: String,
    val tjenestetypeKode: TjenestetypeKode
)

@Serializable
data class HarBorgerFrikortSvar(
    val borgerFnr: String,
    val dato: String,
    val status: CS,
    val svarMelding: String
)

@Serializable
data class HarBorgerFrikortSvarV2(
    val borgerFnr: String,
    val dato: String,
    val tjenestetypeKode: TjenestetypeKode,
    val status: CS,
    val svarMelding: String
)

enum class TjenestetypeKode {
    A, S, B, LE, PO, HS, LR, PR, PS, TB, RH, FT, BR;

    companion object {
        fun TjenestetypeKode.toKithTjenestetypeKode() = no.kith.xmlstds.nav.egenandel.kodeverk._2016_06_10.TjenestetypeKode.valueOf(this.name)
        fun no.kith.xmlstds.nav.egenandel.kodeverk._2016_06_10.TjenestetypeKode.toTjenestetypeKode() = TjenestetypeKode.valueOf(this.name)
    }
}
