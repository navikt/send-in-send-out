package no.nav.emottak.ebms.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import no.nav.security.token.support.v3.tokenValidationSupport

fun Application.configureAuthentication() {
    install(Authentication) {
        tokenValidationSupport(
            no.nav.emottak.auth.AZURE_AD_AUTH,
            no.nav.emottak.auth.AuthConfig.getTokenSupportConfig()
        )
    }
}
