package no.nav.emottak.melding.model

import kotlinx.serialization.Serializable

@Serializable
data class SendInRequest(
    val messageId: String,
    val conversationId: String,
    val payloadId: String,
    val payload: ByteArray,
    val addressing: Addressing,
    val cpaId: String,
    val ebmsProcessing: EbmsProcessing,
    val signedOf: String? = null,
    val requestId: String,
    val partnerId: Long? = null
)

@Serializable
data class SendInResponse(
    val messageId: String,
    val conversationId: String,
    val addressing: Addressing,
    val payload: ByteArray,
    val requestId: String
)

@Serializable
data class ProcessConfig(
    val kryptering: Boolean,
    val komprimering: Boolean,
    val signering: Boolean,
    val internformat: Boolean,
    val validering: Boolean,
    val apprec: Boolean, // Kan denne løsrives?
    val ocspSjekk: Boolean,
    val adapter: String?,
    val errorAction: String?
)

@Serializable
data class EbmsProcessing(
    val test: String = "123",
    val errorAction: String? = null
)

@Serializable
data class Addressing(
    val to: Party,
    val from: Party,
    val service: String,
    val action: String
) {
    fun replyTo(service: String, action: String): Addressing = Addressing(to = from.copy(), from = to.copy(), service, action)
}

@Serializable
data class Header(
    val messageId: String,
    val conversationId: String,
    val cpaId: String,
    val to: Party,
    val from: Party,
    val service: String,
    val action: String
)

@Serializable
data class Party(
    val partyId: List<PartyId>,
    val role: String
)

@Serializable
data class PartyId(
    val type: String,
    val value: String
)
