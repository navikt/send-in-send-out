package no.nav.emottak.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import java.time.Instant
import java.util.GregorianCalendar
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

fun Instant.toXMLGregorianCalendar() = DatatypeFactory.newInstance().newXMLGregorianCalendar(
    GregorianCalendar().apply { this.setTimeInMillis(this@toXMLGregorianCalendar.toEpochMilli()) }
)

fun String.toXmlGregorianCalendar() = DatatypeFactory.newInstance().newXMLGregorianCalendar(this)

fun LocalDate.toXmlGregorianCalendar() = DatatypeFactory.newInstance().newXMLGregorianCalendar(
    GregorianCalendar().apply {
        this.set(
            this@toXmlGregorianCalendar.year,
            this@toXmlGregorianCalendar.monthNumber,
            this@toXmlGregorianCalendar.dayOfMonth
        )
    }
)

fun XMLGregorianCalendar.toLocalDate() = LocalDate(
    year = this.year,
    month = Month(this.month),
    dayOfMonth = this.day
)
