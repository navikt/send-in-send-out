package no.nav.emottak.ebms.plugin

import io.ktor.server.application.Application
import no.nav.emottak.config.Configurator.config

fun Application.configureCoroutineDebugger() {
    if (config().clusterName.isDev()) {
        dev.reformator.stacktracedecoroutinator.runtime.DecoroutinatorRuntime.load()
    }
}
