package no.nav.emottak.util

import java.time.Instant
import java.util.GregorianCalendar
import javax.xml.datatype.DatatypeFactory

fun Instant.toXMLGregorianCalendar() = DatatypeFactory.newInstance().newXMLGregorianCalendar(
    GregorianCalendar().apply { this.setTimeInMillis(this@toXMLGregorianCalendar.toEpochMilli()) }
)

fun String.toXmlGregorianCalendar() = DatatypeFactory.newInstance().newXMLGregorianCalendar(this)
