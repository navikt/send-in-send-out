package no.nav.emottak.ebms.plugin

import io.ktor.server.application.Application
import no.nav.emottak.utils.environment.isProdEnv

fun Application.configureCoroutineDebugger() {
    if (!isProdEnv()) {
        dev.reformator.stacktracedecoroutinator.runtime.DecoroutinatorRuntime.load()
    }
}
