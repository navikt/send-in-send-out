package no.nav.emottak.util

import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import javax.xml.datatype.DatatypeFactory
import kotlin.time.Instant
import java.time.Instant as JavaInstant

class XMLUtilTest {

    // LocalDate.toXmlGregorianCalendar()

    @Test
    fun `LocalDate toXmlGregorianCalendar preserves year month and day`() {
        val date = LocalDate(2023, 11, 20)
        val result = date.toXmlGregorianCalendar()
        assertEquals(2023, result.year)
        assertEquals(11, result.month)
        assertEquals(20, result.day)
    }

    @Test
    fun `LocalDate toXmlGregorianCalendar handles January correctly`() {
        val date = LocalDate(2024, 1, 1)
        val result = date.toXmlGregorianCalendar()
        assertEquals(2024, result.year)
        assertEquals(1, result.month)
        assertEquals(1, result.day)
    }

    @Test
    fun `LocalDate toXmlGregorianCalendar handles December correctly`() {
        val date = LocalDate(2024, 12, 31)
        val result = date.toXmlGregorianCalendar()
        assertEquals(2024, result.year)
        assertEquals(12, result.month)
        assertEquals(31, result.day)
    }

    // XMLGregorianCalendar.toLocalDate()

    @Test
    fun `XMLGregorianCalendar toLocalDate preserves year month and day`() {
        val xmlCal = DatatypeFactory.newInstance().newXMLGregorianCalendarDate(2023, 11, 20, 0)
        val result = xmlCal.toLocalDate()
        assertEquals(LocalDate(2023, 11, 20), result)
    }

    @Test
    fun `toLocalDate is the inverse of toXmlGregorianCalendar`() {
        val original = LocalDate(2023, 6, 15)
        val roundTripped = original.toXmlGregorianCalendar().toLocalDate()
        assertEquals(original, roundTripped)
    }

    // java.time.Instant.toXmlGregorianCalendar()

    @Test
    fun `java time Instant toXmlGregorianCalendar preserves epoch millis`() {
        val epochMillis = 1700476200000L // 2023-11-20T10:30:00Z
        val instant = JavaInstant.ofEpochMilli(epochMillis)
        val result = instant.toXmlGregorianCalendar()
        assertEquals(epochMillis, result.toGregorianCalendar().toInstant().toEpochMilli())
    }

    // kotlin.time.Instant.toXmlGregorianCalendar()

    @Test
    fun `kotlin time Instant toXmlGregorianCalendar preserves epoch millis`() {
        val epochMillis = 1700476200000L // 2023-11-20T10:30:00Z
        val instant = Instant.fromEpochMilliseconds(epochMillis)
        val result = instant.toXmlGregorianCalendar()
        assertEquals(epochMillis, result.toGregorianCalendar().toInstant().toEpochMilli())
    }

    // XMLGregorianCalendar.toKotlinxInstant()

    @Test
    fun `XMLGregorianCalendar toKotlinxInstant preserves epoch millis`() {
        val epochMillis = 1700476200000L // 2023-11-20T10:30:00Z
        val xmlCal = DatatypeFactory.newInstance().newXMLGregorianCalendar(
            java.util.GregorianCalendar().apply { timeInMillis = epochMillis }
        )
        val result = xmlCal.toKotlinxInstant()
        assertEquals(epochMillis, result.toEpochMilliseconds())
    }

    @Test
    fun `toKotlinxInstant is the inverse of kotlin time Instant toXmlGregorianCalendar`() {
        val original = Instant.fromEpochMilliseconds(1700476200000L)
        val inverse = original.toXmlGregorianCalendar().toKotlinxInstant()
        assertEquals(original, inverse)
    }
}
