package no.nav.emottak.util

import org.junit.jupiter.api.Test

class PidValidatorTest {

    @Test
    fun `IsValidPid should accept FNR`() = assert(PidValidator.isValidPid("13097248022"))

    // dnr is identical to fnr except for the first digit
    @Test
    fun `IsValidPid should accept DNR`() = assert(PidValidator.isValidPid("53097248016"))

    // hnr is identical to fnr except for the third digit
    @Test
    fun `IsValidPid should accept HNR`() = assert(PidValidator.isValidPid("13527248013"))

    @Test
    fun `IsValidFnr should accept FNR`() = assert(PidValidator.isValidFnr("13097248022"))

    @Test
    fun `IsValidFnr should not accept DNR`() = assert(!PidValidator.isValidFnr("53097248016"))

    @Test
    fun `IsValidFnr should not accept HNR`() = assert(!PidValidator.isValidFnr("13527248013"))

    @Test
    fun `Should reject if less than 11 digits`() = assert(!PidValidator.isValidPid("1234567890"))

    @Test
    fun `Should reject if more than 11 digits`() = assert(!PidValidator.isValidPid("123456789012"))

    @Test
    fun `Should reject if checksum 1 is invalid`() = assert(!PidValidator.isValidPid("13097248032"))

    @Test
    fun `Should reject if checksum 2 is invalid`() = assert(!PidValidator.isValidPid("13097248023"))

    // NB: Does not validate leap years or if the month actually have 31 days
    @Test
    fun `Should reject if day is invalid`() = assert(!PidValidator.isValidPid("32049648742"))

    @Test
    fun `Should reject if month is invalid`() = assert(!PidValidator.isValidPid("13137248022"))
}
