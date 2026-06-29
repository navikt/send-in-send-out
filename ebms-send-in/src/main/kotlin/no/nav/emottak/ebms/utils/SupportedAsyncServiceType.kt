package no.nav.emottak.ebms.utils

enum class SupportedAsyncServiceType(val service: String) {
    Trekkopplysning("Trekkopplysning"),
    Sykmelding("Sykmelding"),
    Legemelding("Legemelding"),
    BehandlerKrav("BehandlerKrav"),
    OppgjorsKontroll("OppgjørsKontroll"),
    DialogmoteInnkalling("DialogmoteInnkalling"),
    ForesporselFraSaksbehandler("ForesporselFraSaksbehandler"),
    HenvendelseFraLege("HenvendelseFraLege"),
    HenvendelseFraSaksbehandler("HenvendelseFraSaksbehandler"),
    Oppfolgingsplan("Oppfølgingsplan"),
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

// Actions som er nødvendige for å identifisere hvilken MQ som skal brukes
enum class AsyncRoutingAction(val action: String, val service: SupportedAsyncServiceType) {
    OppgjorsKontrollSvarmelding("Svarmelding", SupportedAsyncServiceType.OppgjorsKontroll),
    OppgjorsKontrollOppgjorskrav("Oppgjorskrav", SupportedAsyncServiceType.OppgjorsKontroll),
    DialogmoteInnkallingMoteRespons("MoteRespons", SupportedAsyncServiceType.DialogmoteInnkalling),
    DialogmoteInnkallingKvittering("Kvittering", SupportedAsyncServiceType.DialogmoteInnkalling),
    ForesporselFraSaksbehandlerForesporselSvar("ForesporselSvar", SupportedAsyncServiceType.ForesporselFraSaksbehandler),
    ForesporselFraSaksbehandlerKvittering("Kvittering", SupportedAsyncServiceType.ForesporselFraSaksbehandler),
    Unsupported("Unsupported", SupportedAsyncServiceType.Unsupported);

    companion object {
        fun String.toAsyncRoutingAction(service: SupportedAsyncServiceType): AsyncRoutingAction {
            AsyncRoutingAction.entries.forEach { asyncRoutingAction ->
                if (asyncRoutingAction.service == service && asyncRoutingAction.action == this) return asyncRoutingAction
            }
            return AsyncRoutingAction.Unsupported
        }
    }
}
