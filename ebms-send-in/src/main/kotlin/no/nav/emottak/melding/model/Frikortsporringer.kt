package no.nav.emottak.melding.model
import kotlinx.serialization.Serializable

@Serializable
data class FrikortsporringRequest(
    val eiFellesformat: EIFellesformat
)

@Serializable
data class EIFellesformat(
    val msgHead: MsgHead? = null,
    val mottakenhetBlokk: MottakenhetBlokk? = null
)

@Serializable
data class MsgHead(
    val msgInfo: MsgInfo,
    val documents: List<Document>? = null
)

@Serializable
data class MottakenhetBlokk(
    val ebAction: String? = null,
    val ebService: String? = null,
    val ebRole: String? = null,
    val ebXMLSamtaleId: String? = null,
    val ediLoggId: String? = null,
    val mottaksId: String? = null,
    val partnerReferanse: String? = null
)

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
)

@Serializable
data class ConversationRef(
    val refToParent: String,
    val refToConversation: String
)

@Serializable
data class CS(
    val v: String? = null,
    val dn: String? = null
)

@Serializable
data class Sender(
    val organization: Organization,
    val comMethod: CS? = null
)

@Serializable
data class Receiver(
    val organization: Organization,
    val comMethod: CS? = null
)

@Serializable
data class Organization(
    val organizationName: String? = null,
    val ident: List<Ident>? = null,
    val healthcareProfessional: HealthcareProfessional? = null
)

@Serializable
data class Ident(
    val id: String,
    val typeId: CV
)

@Serializable
data class HealthcareProfessional(
    val familyName: String? = null,
    val givenName: String? = null,
    val ident: List<Ident>? = null
)

@Serializable
data class CV(
    val v: String? = null,
    val dn: String? = null,
    val s: String? = null
)

@Serializable
data class Document(
    val documentConnection: CS? = null,
    val refDoc: RefDoc
)

@Serializable
data class RefDoc(
    val msgType: CS,
    val content: Content? = null,
    val mimeType: String? = null
)

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
)

@Serializable
data class EgenandelForesporsel(
    val harBorgerFrikort: HarBorgerFrikortParamType? = null,
    val harBorgerEgenandelfritak: HarBorgerEgenandelfritakParamType? = null
)

@Serializable
data class EgenandelForesporselV2(
    val harBorgerFrikort: HarBorgerFrikortParamTypeV2? = null,
    val harBorgerEgenandelfritak: HarBorgerEgenandelfritakParamType? = null
)

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
)

@Serializable
data class HarBorgerFrikortParamTypeV2(
    val borgerFnr: String,
    val dato: String,
    val tjenestetypeKode: TjenestetypeKode
)

@Serializable
data class HarBorgerEgenandelfritakParamType(
    val borgerFnr: String,
    val dato: String
)

@Serializable
data class EgenandelSvar(
    val status: CS,
    val svarMelding: String
)

@Serializable
data class EgenandelSvarV2(
    val status: CS,
    val svarMelding: String
)

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
    A, S, B, LE, PO, HS, LR, PR, PS, TB, RH, FT, BR
}
