package no.nav.emottak.util

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import java.util.GregorianCalendar
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

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
            this@toXmlGregorianCalendar.month.value,
            this@toXmlGregorianCalendar.dayOfMonth
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
