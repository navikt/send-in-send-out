package no.nav.emottak.config

import no.nav.emottak.utils.config.Kafka
import kotlin.time.Duration

data class Config(
    val server: Server,
    val kafka: Kafka
)

data class Server(
    val port: Int,
    val preWait: Duration
)
