package no.nav.emottak.config

import no.nav.emottak.utils.config.EventLogging
import no.nav.emottak.utils.config.Kafka
import no.nav.emottak.utils.config.Server

data class Config(
    val server: Server,
    val kafka: Kafka,
    val eventLogging: EventLogging,
    val frikortCpalist: List<String>,
    val cluster: ClusterName
)

@JvmInline
value class ClusterName(val value: String) {
    fun isDev() = value == "dev-fss"
    fun isProd() = value == "prod-fss"
}
