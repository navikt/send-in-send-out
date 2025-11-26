package no.nav.emottak.config

import arrow.core.memoize
import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.addResourceSource

val config: () -> Config = {
    ConfigLoader.builder()
        .addResourceSource("/application-personal.conf", optional = true)
        .addResourceSource("/kafka_common.conf")
        .addResourceSource("/application.conf")
        .addResourceSource("/frikort_rest_cpalist.conf")
        .withExplicitSealedTypes()
        .build()
        .loadConfigOrThrow<Config>()
}.memoize()
