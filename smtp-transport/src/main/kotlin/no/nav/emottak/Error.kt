package no.nav.emottak

sealed interface Error

data class PayloadAlreadyExist(
    val referenceId: String,
    val contentId: String
) : Error

data class PayloadDoesNotExist(
    val referenceId: String,
    val contentId: String? = null
) : Error

sealed interface PayloadRequestValidationError : Error
data object EmptyReferenceId: PayloadRequestValidationError
data object EmptyContentId: PayloadRequestValidationError

sealed interface NotValidUUID : PayloadRequestValidationError
data class InvalidReferenceId(val referenceId: String) : NotValidUUID
data class InvalidContentId(val contentId: String) : NotValidUUID

fun EmptyReferenceId.toString() = "ReferenceId cannot be empty"
fun EmptyContentId.toString() = "ContentId cannot be empty"
fun InvalidReferenceId.toString() = "ReferenceId is not a valid UUID: '$referenceId'"
fun InvalidContentId.toString() = "ContentId is not a valid UUID: '$contentId'"