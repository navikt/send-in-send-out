package no.nav.emottak.ebms.route

import io.ktor.http.HttpStatusCode
import no.nav.emottak.ebms.service.UnsupportedAsyncServiceException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AsyncHttpErrorStatusTest {
    @Test
    fun `unsupported service maps to bad request`() {
        assertEquals(
            HttpStatusCode.BadRequest,
            UnsupportedAsyncServiceException("Unsupported service").toAsyncHttpStatusCode()
        )
    }

    @Test
    fun `unexpected processing failure maps to internal server error`() {
        assertEquals(
            HttpStatusCode.InternalServerError,
            RuntimeException("Backend unavailable").toAsyncHttpStatusCode()
        )
    }
}
