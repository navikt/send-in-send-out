package no.nav.emottak.ebms.utils

internal enum class SupportedServiceType(val service: String) {
    HarBorgerEgenandelFritak("HarBorgerEgenandelFritak"),
    HarBorgerFrikort("HarBorgerFrikort"),
    HarBorgerFrikortMengde("HarBorgerFrikortMengde"),
    Inntektsforesporsel("Inntektsforesporsel"),
    PasientlisteForesporsel("PasientlisteForesporsel"),
    Trekkopplysning("Trekkopplysning"),
    Unsupported("Unsupported");

    companion object {
        fun String.toSupportedService(): SupportedServiceType = try {
            SupportedServiceType.valueOf(this)
        } catch (e: IllegalArgumentException) {
            Unsupported
        }
        fun SupportedServiceType.isSupportedService(): Boolean = this != Unsupported
    }
}
