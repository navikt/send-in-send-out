package no.nav.emottak.repository

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.raise.zipOrAccumulate
import no.nav.emottak.EmptyContentId
import no.nav.emottak.EmptyReferenceId
import no.nav.emottak.InvalidContentId
import no.nav.emottak.InvalidReferenceId
import no.nav.emottak.PayloadRequestValidationError
import org.apache.kafka.common.Uuid

data class PayloadRequest private constructor(val referenceId: String, val contentId: String? = null) {
    companion object {
        operator fun invoke(
            referenceId: String, contentId: String? = null
        ): Either<NonEmptyList<PayloadRequestValidationError>, PayloadRequest> = either {
            zipOrAccumulate(
                { // Validering av referenceId:
                    ensure(referenceId.isNotEmpty()) { EmptyReferenceId }
                    ensureNotNull(Uuid.fromString(referenceId)) { InvalidReferenceId(referenceId) }
                },
                { // Validering av contentId hvis den ikke er null:
                    if (contentId != null) {
                        ensure(contentId.isNotEmpty()) { EmptyContentId }
                        ensureNotNull(Uuid.fromString(contentId)) { InvalidContentId(contentId) } // TODO: Er contentId også UUID?
                    }
                }
            ) { _, _ -> Unit }
            PayloadRequest(referenceId, contentId)
        }
    }
}

