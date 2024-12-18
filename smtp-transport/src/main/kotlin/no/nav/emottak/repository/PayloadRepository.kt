package no.nav.emottak.repository

import arrow.core.raise.Raise
import arrow.core.raise.catch
import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import no.nav.emottak.Error.PayloadAlreadyExist
import no.nav.emottak.smtp.PayloadDatabase
import no.nav.emottak.util.Payload
import java.sql.SQLException
import java.sql.SQLIntegrityConstraintViolationException

private const val INCOMING = "IN"

private typealias UniqueViolation = SQLIntegrityConstraintViolationException

class PayloadRepository(payloadDatabase: PayloadDatabase) {
    private val payloadQueries = payloadDatabase.payloadQueries

    suspend fun Raise<PayloadAlreadyExist>.insert(payloads: List<Payload>): List<Pair<String, String>> =
        withContext(IO) {
            payloadQueries.transactionWithResult {
                payloads.map { payload -> insertPayload(payload) }
            }
        }

    private fun Raise<PayloadAlreadyExist>.insertPayload(payload: Payload): Pair<String, String> =
        catch({
            val inserted = payloadQueries.insertPayload(
                reference_id = payload.referenceId,
                content_id = payload.contentId,
                content_type = payload.contentType,
                content = payload.content,
                direction = INCOMING
            )
                .executeAsOne()

            return Pair(
                inserted.reference_id,
                inserted.content_id
            )
        }) { e: SQLException ->
            if (e is UniqueViolation) {
                raise(
                    PayloadAlreadyExist(
                        payload.referenceId,
                        payload.contentId
                    )
                )
            } else {
                throw e
            }
        }

    /*
    suspend fun Raise<NotFoundException>.getPayload(referenceId: String): Payload =
        withContext(IO) {
            payloadQueries.transactionWithResult {
                catch({
                    val payload: no.nav.emottak.smtp.Payload? = payloadQueries.getPayload(
                        reference_id = referenceId,
                    ).executeAsOneOrNull()
                    Payload(
                        referenceId = payload!!.reference_id,
                        contentId = payload.content_id,
                        contentType = payload.content_type,
                        content = payload.content
                        // TODO: Trenger vi feltene direction og/eller created_at?
                    )
                }) { _: SQLException ->
                    raise(
                        NotFoundException("Fant ikke payload.reference_id $referenceId")
                    )
                }
            }
        }
    */

    // TODO: Skal denne egentlig være suspend?
    fun getPayload(referenceId: String): Payload =
        payloadQueries.transactionWithResult {
            val payload: no.nav.emottak.smtp.Payload? = payloadQueries.getPayload(
                reference_id = referenceId
            ).executeAsOneOrNull()
            if (payload != null) Payload(
                referenceId = payload.reference_id,
                contentId = payload.content_id,
                contentType = payload.content_type,
                content = payload.content
                // TODO: Trenger vi feltene direction og/eller created_at?
            ) else throw NotFoundException()
        }
}
