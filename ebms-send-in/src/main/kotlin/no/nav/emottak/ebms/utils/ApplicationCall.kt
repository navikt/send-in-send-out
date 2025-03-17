package no.nav.emottak.ebms.utils

import arrow.core.Either
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive

suspend inline fun <reified T : Any> ApplicationCall.receiveEither(): Either<Throwable, T> =
    Either.catch { receive(T::class) }
