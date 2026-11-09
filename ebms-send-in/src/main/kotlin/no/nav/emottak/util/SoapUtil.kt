package no.nav.emottak.util

import org.apache.cxf.binding.soap.SoapFault
import javax.xml.ws.soap.SOAPFaultException

internal fun Throwable.isRequestSoapFault(): Boolean =
    soapFaultCode() in setOf("Client", "Sender")

internal fun Throwable.isServerSoapFault(): Boolean =
    soapFaultCode() in setOf("Server", "Receiver")

internal fun Throwable.soapFaultString(): String? =
    when (this) {
        is SoapFault -> reason.takeIf { it.isNotBlank() }
        is SOAPFaultException -> fault?.faultString?.takeIf { it.isNotBlank() }
        else -> null
    }

private fun Throwable.soapFaultCode(): String? =
    when (this) {
        is SoapFault -> faultCode?.localPart
        is SOAPFaultException -> fault?.faultCode?.substringAfterLast(':')
        else -> null
    }
