package no.nav.emottak.ebms

internal enum class SupportedService(val service: String) {
    HarBorgerEgenandelFritak("HarBorgerEgenandelFritak"),
    HarBorgerFrikort("HarBorgerFrikort"),
    HarBorgerFrikortMengde("HarBorgerFrikortMengde"),
    Inntektsforesporsel("Inntektsforesporsel"),
    PasientlisteForesporsel("PasientlisteForesporsel"),
    Unsupported("Unsupported");

    companion object {
        fun String.toSupportedService(): SupportedService = try {
            SupportedService.valueOf(this)
        } catch (e: IllegalArgumentException) {
            Unsupported
        }
        fun SupportedService.isSupportedService(): Boolean = this != Unsupported
    }
}
