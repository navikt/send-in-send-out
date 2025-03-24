package no.nav.emottak.util

// Based on fnrValideringUtil.ts from https://github.com/navikt/finnfastlege
abstract class PidValidator {

    companion object {

        fun isValidPid(pid: String): Boolean {
            return is11Numbers(pid) && isValidFodselsdato(pid) && hasValidControlNumbers(pid)
        }

        fun isValidFnr(fnr: String): Boolean {
            if (!is11Numbers(fnr)) {
                return false
            }
            if (!isValidPNumber(fnr.getDay(decimalRadix), fnr.getMonth(decimalRadix))) {
                return false
            }
            return hasValidControlNumbers(fnr)
        }

        private val kontrollRekke1 = listOf(3, 7, 6, 1, 8, 9, 4, 5, 2)
        private val kontrollRekke2 = listOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2)
        private const val decimalRadix = 10

        private fun is11Numbers(pid: String) = pid.matches(Regex("^[0-9]{11}\$"))

        private fun isValidPNumber(day: Int, month: Int) = day > 0 && day <= 32 && month > 0 && month <= 12

        private fun isValidDNumber(day: Int, month: Int) = day > 40 && day <= 72 && month > 0 && month <= 12

        private fun isValidHNumber(day: Int, month: Int) = day > 0 && day <= 32 && month > 40 && month <= 52

        private fun isValidDate(dag: Int, maned: Int) =
            isValidPNumber(dag, maned) || isValidDNumber(dag, maned) || isValidHNumber(dag, maned)

        private fun isValidFodselsdato(pid: String): Boolean {
            return isValidDate(pid.getDay(decimalRadix), pid.getMonth(decimalRadix))
        }

        private fun hasValidControlNumbers(pid: String): Boolean {
            val pidList = pid.map { it.toString().toInt(decimalRadix) }
            val controlNumber1 = getControlNumber(pidList.take(9), kontrollRekke1)
            val controlNumber2 = getControlNumber(pidList.take(10), kontrollRekke2)
            return pidList[9] == controlNumber1 && pidList[10] == controlNumber2
        }

        private fun getControlNumber(pidList: List<Int>, kontrollrekke: List<Int>): Int {
            var sum = 0
            for (i in pidList.indices) {
                sum += pidList[i] * kontrollrekke[i]
            }
            val controlNumber = sum % 11
            return if (controlNumber != 0) 11 - controlNumber else 0
        }
    }
}

private fun String.getDay(decimalRadix: Int = 10) = this.substring(0, 2).toInt(decimalRadix)
private fun String.getMonth(decimalRadix: Int = 10) = this.substring(2, 4).toInt(decimalRadix)
