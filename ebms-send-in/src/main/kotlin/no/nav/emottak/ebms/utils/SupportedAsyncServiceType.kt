package no.nav.emottak.ebms.utils

internal enum class SupportedAsyncServiceType(val service: String) {
    Trekkopplysning("Trekkopplysning"),
    Unsupported("Unsupported");

    companion object {
        fun String.toSupportedAsyncService(): SupportedAsyncServiceType = try {
            SupportedAsyncServiceType.valueOf(this)
        } catch (e: IllegalArgumentException) {
            Unsupported
        }
        fun SupportedAsyncServiceType.isSupportedService(): Boolean = this != Unsupported
    }
}
