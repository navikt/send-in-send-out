package no.nav.emottak.ebms.route

import io.ktor.http.HttpStatusCode
import no.nav.emottak.ebms.service.UnsupportedAsyncServiceException
import no.nav.emottak.util.isRequestSoapFault
import org.apache.cxf.binding.soap.SoapFault
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import javax.xml.namespace.QName

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

    @Test
    fun `SOAP server fault maps to internal server error for synchronous requests`() {
        val fault = SoapFault(
            "Backend unavailable",
            QName("http://schemas.xmlsoap.org/soap/envelope/", "Server")
        )

        assertEquals(HttpStatusCode.InternalServerError, fault.toSyncHttpStatusCode())
    }

    @Test
    fun `SOAP client fault is identified as a request fault`() {
        val fault = SoapFault(
            "Invalid request",
            QName("http://schemas.xmlsoap.org/soap/envelope/", "Client")
        )

        assertTrue(fault.isRequestSoapFault())
    }
}
