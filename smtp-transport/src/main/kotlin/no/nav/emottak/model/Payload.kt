package no.nav.emottak.model

data class Payload(
    val referenceId: String,
    val contentId: String,
    val contentType: String,
    val content: ByteArray
)
