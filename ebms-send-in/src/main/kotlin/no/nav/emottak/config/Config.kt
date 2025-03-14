package no.nav.emottak.config

import kotlin.time.Duration

data class Config(
    val server: Server
)

data class Server(
    val port: Int,
    val preWait: Duration,
    val maxChunkSize: String
)
