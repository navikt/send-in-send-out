package no.nav.emottak

import arrow.fx.coroutines.ExitCase
import arrow.fx.coroutines.ResourceScope
import io.micrometer.prometheus.PrometheusConfig.DEFAULT
import io.micrometer.prometheus.PrometheusMeterRegistry

suspend fun ResourceScope.metricsRegistry(): PrometheusMeterRegistry =
    install({ PrometheusMeterRegistry((DEFAULT)) }) { p, _: ExitCase -> p.close() }
