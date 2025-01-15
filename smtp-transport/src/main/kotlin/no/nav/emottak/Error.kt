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

data object EmptyReferenceId : PayloadRequestValidationError {
    override fun toString() = "ReferenceId cannot be empty"
}
data object EmptyContentId : PayloadRequestValidationError {
    override fun toString() = "ContentId cannot be empty"
}

sealed interface NotValidUUID : PayloadRequestValidationError

data class InvalidReferenceId(val referenceId: String) : NotValidUUID {
    override fun toString() = "ReferenceId is not a valid UUID: '$referenceId'"
}
data class InvalidContentId(val contentId: String) : NotValidUUID {
    override fun toString() = "ContentId is not a valid UUID: '$contentId'"
}
