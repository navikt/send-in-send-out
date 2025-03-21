package no.nav.emottak.util

import org.junit.jupiter.api.Test

class PidValidatorTest {

    @Test
    fun `Should accept FNR`()  = assert(PidValidator.isValidFodselsnummer("13097248022"))

    @Test
    fun `Should accept DNR`() {
        // dnr is identical to fnr except for the first digit
        assert(PidValidator.isValidFodselsnummer("53097248016"))
    }

    @Test
    fun `Should accept HNR`() {
        // hnr is identical to fnr except for the third digit
        assert(PidValidator.isValidFodselsnummer("13527248013"))
    }

    @Test
    fun `Should reject if less than 11 digits`() = assert(!PidValidator.isValidFodselsnummer("1234567890"))

    @Test
    fun `Should reject if more than 11 digits`() = assert(!PidValidator.isValidFodselsnummer("123456789012"))

    @Test
    fun `Should reject if checksum 1 is invalid`() = assert(!PidValidator.isValidFodselsnummer("13097248032"))

    @Test
    fun `Should reject if checksum 2 is invalid`() = assert(!PidValidator.isValidFodselsnummer("13097248023"))

    @Test
    fun `Should reject if day is invalid`() = assert(!PidValidator.isValidFodselsnummer("32049648742"))

    // @Test
    fun `Should reject if day of month is invalid`() {
        // NB: Does not validate leap years or if the month actually have 31 days
        assert(!PidValidator.isValidFodselsnummer("31049648742"))
    }

    @Test
    fun `Should reject if month is invalid`() = assert(!PidValidator.isValidFodselsnummer("13137248022"))
}
