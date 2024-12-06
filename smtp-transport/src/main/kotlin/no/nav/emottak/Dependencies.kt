package no.nav.emottak

import arrow.core.memoize
import arrow.fx.coroutines.ExitCase
import arrow.fx.coroutines.ResourceScope
import arrow.fx.coroutines.parZip
import io.github.nomisRev.kafka.publisher.PublisherSettings
import io.micrometer.prometheus.PrometheusConfig.DEFAULT
import io.micrometer.prometheus.PrometheusMeterRegistry
import jakarta.mail.Authenticator
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Store
import no.nav.emottak.configuration.Config
import no.nav.emottak.configuration.Kafka
import no.nav.emottak.configuration.Smtp
import no.nav.emottak.configuration.toProperties
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer

data class Dependencies(
    val store: Store,
    val session: Session,
    val publisherSettings: PublisherSettings<String, ByteArray>,
    val meterRegistry: PrometheusMeterRegistry
)

private suspend fun ResourceScope.metricsRegistry(): PrometheusMeterRegistry =
    install({ PrometheusMeterRegistry(DEFAULT) }) { p, _: ExitCase ->
        p.close().also { log.info("Closed prometheus registry") }
    }

private suspend fun ResourceScope.store(smtp: Smtp): Store =
    install({ session(smtp).getStore(smtp.storeProtocol.value).also { it.connect() } }) { s, _: ExitCase ->
        s.close().also { log.info("Closed session store") }
    }

fun kafkaPublisherSettings(kafka: Kafka): PublisherSettings<String, ByteArray> =
    PublisherSettings(
        bootstrapServers = kafka.bootstrapServers,
        keySerializer = StringSerializer(),
        valueSerializer = ByteArraySerializer()
    )

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

suspend fun ResourceScope.initDependencies(config: Config) =
    parZip(
        { store(config.smtp) },
        { kafkaPublisherSettings(config.kafka) },
        { metricsRegistry() }
    ) { store, kafkaPublisher, metricsRegistry ->
        Dependencies(store, session(config.smtp), kafkaPublisher, metricsRegistry)
    }
