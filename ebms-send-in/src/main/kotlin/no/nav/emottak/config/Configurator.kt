package no.nav.emottak.config

import arrow.core.memoize
import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.addResourceSource

object Configurator {
    private var configMemoized: (() -> Config)? = null

    val config: () -> Config
        get() {
            if (configMemoized == null) {
                configMemoized = {
                    ConfigLoader.builder()
                        .addResourceSource("/application-personal.conf", optional = true)
                        .addResourceSource("/kafka_common.conf")
                        .addResourceSource("/application.conf")
                        .withExplicitSealedTypes()
                        .build()
                        .loadConfigOrThrow<Config>()
                }.memoize()
            }
            return configMemoized!!
        }

    fun resetMemoizedConfig() {
        configMemoized = null
    }
}
