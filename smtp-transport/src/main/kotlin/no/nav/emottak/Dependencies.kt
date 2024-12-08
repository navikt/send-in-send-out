package no.nav.emottak

import arrow.core.memoize
import arrow.fx.coroutines.ExitCase
import arrow.fx.coroutines.ResourceScope
import arrow.fx.coroutines.parZip
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.micrometer.prometheus.PrometheusConfig.DEFAULT
import io.micrometer.prometheus.PrometheusMeterRegistry
import jakarta.mail.Authenticator
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Store
import no.nav.emottak.configuration.Config
import no.nav.emottak.configuration.Smtp
import no.nav.emottak.configuration.toProperties
import no.nav.emottak.smtp.log

data class Dependencies(
    val store: Store,
    val session: Session,
    val httpClient: HttpClient,
    val meterRegistry: PrometheusMeterRegistry
)

suspend fun ResourceScope.httpClient(): HttpClient =
    install({ HttpClient(CIO) }) { h, _: ExitCase -> h.close().also { log.info("Closed http client") } }

suspend fun ResourceScope.metricsRegistry(): PrometheusMeterRegistry =
    install({ PrometheusMeterRegistry(DEFAULT) }) { p, _: ExitCase ->
        p.close().also { log.info("Closed prometheus registry") }
    }

suspend fun ResourceScope.store(smtp: Smtp): Store =
    install({ session(smtp).getStore(smtp.storeProtocol.value).also { it.connect() } }) { s, _: ExitCase ->
        s.close().also { log.info("Closed session store") }
    }

suspend fun ResourceScope.initDependencies(config: Config) =
    parZip({ store(config.smtp) }, { httpClient() }, { metricsRegistry() }) { store, httpClient, metricsRegistry ->
        Dependencies(store, session(config.smtp), httpClient, metricsRegistry)
    }

private val session: (Smtp) -> Session = { smtp: Smtp ->
    Session.getInstance(
        smtp.toProperties(),
        object : Authenticator() {
            override fun getPasswordAuthentication() = PasswordAuthentication(
                smtp.username.value,
                smtp.password.value
            )
        }
    )
}
    .memoize()
