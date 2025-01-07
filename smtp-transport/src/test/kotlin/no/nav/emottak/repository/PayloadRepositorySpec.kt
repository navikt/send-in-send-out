package no.nav.emottak.repository

import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.raise.either
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import no.nav.emottak.Error.PayloadAlreadyExist
import no.nav.emottak.Error.PayloadDoesNotExist
import no.nav.emottak.payloadDatabase
import no.nav.emottak.util.Payload

class PayloadRepositorySpec : StringSpec(
    {
        val repository = PayloadRepository(payloadDatabase())

        "should insert single payload" {
            val payloads = createSinglePayload()

            with(repository) {
                either { insert(payloads) } shouldBe Right(
                    listOf(Pair("reference-id", "content-id"))
                )
            }
        }

        "should insert multiple payloads" {
            val payloads = createMultiplePayloads()

            with(repository) {
                either { insert(payloads) } shouldBe Right(
                    listOf(
                        Pair("first-reference-id", "first-content-id"),
                        Pair("second-reference-id", "second-content-id")
                    )
                )
            }
        }

        "should insert and retrieve a single payload" {
            val payload = Payload("ref", "cont", "t", "d".toByteArray())

            with(repository) {
                either { insert(listOf(payload)) }

                val eitherPayload = either { retrieve("ref", "cont") }
                val retrievedPayload = eitherPayload.shouldBeRight()

                retrievedPayload.referenceId shouldBe payload.referenceId
                retrievedPayload.contentId shouldBe payload.contentId
                retrievedPayload.contentType shouldBe payload.contentType
                retrievedPayload.content shouldBe payload.content
            }
        }

        "should fail on non existing payload" {
            with(repository) {
                either { retrieve("no-ref-id", "no-content-id") } shouldBe Left(
                    PayloadDoesNotExist(
                        "no-ref-id",
                        "no-content-id"
                    )
                )
            }
        }

        "should fail on duplicate payload" {
            val payloads = createDuplicatePayloads()

            with(repository) {
                either { insert(payloads) } shouldBe Left(
                    PayloadAlreadyExist(
                        "duplicate-reference-id",
                        "duplicate-content-id"
                    )
                )
            }
        }
    }
)

private fun createSinglePayload() = listOf(
    Payload(
        "reference-id",
        "content-id",
        "text",
        "data".toByteArray()
    )
)

private fun createMultiplePayloads() = listOf(
    Payload(
        "first-reference-id",
        "first-content-id",
        "text",
        "first".toByteArray()
    ),
    Payload(
        "second-reference-id",
        "second-content-id",
        "text",
        "second".toByteArray()
    )
)

private fun createDuplicatePayloads() = listOf(
    Payload(
        "duplicate-reference-id",
        "duplicate-content-id",
        "text",
        "duplicate".toByteArray()
    ),
    Payload(
        "duplicate-reference-id",
        "duplicate-content-id",
        "text",
        "duplicate".toByteArray()
    )
)
