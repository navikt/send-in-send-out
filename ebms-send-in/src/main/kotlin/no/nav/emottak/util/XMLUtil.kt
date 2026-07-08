package no.nav.emottak.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import java.util.GregorianCalendar
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar
import kotlin.time.Instant

fun java.time.Instant.toXmlGregorianCalendar() = DatatypeFactory.newInstance().newXMLGregorianCalendar(
    GregorianCalendar().apply { this.setTimeInMillis(this@toXmlGregorianCalendar.toEpochMilli()) }
)

fun Instant.toXmlGregorianCalendar() = DatatypeFactory.newInstance().newXMLGregorianCalendar(
    GregorianCalendar().apply { this.setTimeInMillis(this@toXmlGregorianCalendar.toEpochMilliseconds()) }
)

fun LocalDate.toXmlGregorianCalendar() = DatatypeFactory.newInstance().newXMLGregorianCalendar(
    GregorianCalendar().apply {
        this.set(
            this@toXmlGregorianCalendar.year,
            this@toXmlGregorianCalendar.month.number - 1, // XMLGregorianCalendar months are 0-based, while kotlinx.datetime months are 1-based
            this@toXmlGregorianCalendar.day
        )
    }
)

fun XMLGregorianCalendar.toLocalDate() = LocalDate(
    this.year,
    this.month,
    this.day
)

fun XMLGregorianCalendar.toKotlinxInstant(): Instant =
    Instant.fromEpochMilliseconds(this.toGregorianCalendar().toInstant().toEpochMilli())
