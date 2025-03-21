package no.nav.emottak.util

// Based on fnrValideringUtil.ts from https://github.com/navikt/finnfastlege
abstract class PidValidator {

    companion object {

        fun isValidFodselsnummer(fodselsnummer: String): Boolean {
            if (!fodselsnummer.matches(Regex("^[0-9]{11}\$"))) {
                return false
            }
            if (!isValidFodselsdato(fodselsnummer.substring(0, 6))) {
                return false
            }
            val fodselsnummerListe = fodselsnummer.map { it.toString().toInt(decimalRadix) }
            val kontrollSiffer1 = hentKontrollSiffer(fodselsnummerListe.take(9), kontrollRekke1)
            val kontrollSiffer2 = hentKontrollSiffer(fodselsnummerListe.take(10), kontrollRekke2)
            return fodselsnummerListe[9] == kontrollSiffer1 && fodselsnummerListe[10] == kontrollSiffer2
        }

        private val kontrollRekke1 = listOf(3, 7, 6, 1, 8, 9, 4, 5, 2)
        private val kontrollRekke2 = listOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2)
        private const val decimalRadix = 10

        private fun isValidPNumber(dag: Int, maned: Int) = dag > 0 && dag <= 32 && maned > 0 && maned <= 12

        private fun isValidDNumber(dag: Int, maned: Int) = dag > 40 && dag <= 72 && maned > 0 && maned <= 12

        private fun isValidHNumber(dag: Int, maned: Int) = dag > 0 && dag <= 32 && maned > 40 && maned <= 52

        private fun isValidDate(dag: Int, maned: Int) =
            isValidPNumber(dag, maned) || isValidDNumber(dag, maned) || isValidHNumber(dag, maned)

        private fun isValidFodselsdato(fodselsnummer: String): Boolean {
            val dag = fodselsnummer.substring(0, 2).toInt(decimalRadix)
            val maned = fodselsnummer.substring(2, 4).toInt(decimalRadix)
            return isValidDate(dag, maned)
        }

        private fun hentKontrollSiffer(fodselsnummer: List<Int>, kontrollrekke: List<Int>): Int {
            var sum = 0
            for (i in fodselsnummer.indices) {
                sum += fodselsnummer[i] * kontrollrekke[i]
            }
            val kontrollSiffer = sum % 11
            return if (kontrollSiffer != 0) 11 - kontrollSiffer else 0
        }
    }
}
