package no.nav.emottak.ebms.utils

internal enum class SupportedSyncServiceType(val service: String) {
    HarBorgerEgenandelFritak("HarBorgerEgenandelFritak"),
    HarBorgerFrikort("HarBorgerFrikort"),
    HarBorgerFrikortMengde("HarBorgerFrikortMengde"),
    Inntektsforesporsel("Inntektsforesporsel"),
    Unsupported("Unsupported");

    companion object {
        fun String.toSupportedService(): SupportedSyncServiceType = try {
            SupportedSyncServiceType.valueOf(this)
        } catch (e: IllegalArgumentException) {
            Unsupported
        }
        fun SupportedSyncServiceType.isSupportedService(): Boolean = this != Unsupported
    }
}
