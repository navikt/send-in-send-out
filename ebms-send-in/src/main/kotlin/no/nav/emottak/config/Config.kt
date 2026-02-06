package no.nav.emottak.config

import no.nav.emottak.utils.config.EventLogging
import no.nav.emottak.utils.config.Kafka
import no.nav.emottak.utils.config.Server

data class Config(
    val server: Server,
    val kafka: Kafka,
    val ebmsInPayloadReceiver: KafkaReceiverConfig,
    val eventLogging: EventLogging,
    val clusterName: ClusterName,
    val frikorttjenester: FrikortTjenester,
    val frikortCpalist: Set<String>,
    val azureAuth: AzureAuth
)

data class KafkaReceiverConfig(
    val active: Boolean,
    val topic: String
)

@JvmInline
value class ClusterName(val value: String) {
    fun isDev() = value == "dev-fss"
    fun isProd() = value == "prod-fss"
}

data class FrikortTjenester(
    val hostname: Host,
    val scope: AppScope,
    val harBorgerFrikortEndpoint: Url,
    val harBorgerEgenandelFritakEndpoint: Url,
    val pingEndpoint: Url
)

@JvmInline
value class Host(val value: String)

@JvmInline
value class Url(val value: String)

@JvmInline
value class AzureAd(val value: String)

@JvmInline
value class AppScope(val value: String)

@JvmInline
value class AzureHttpProxy(val value: String)

@JvmInline
value class AzureAdAuth(val value: String)

@JvmInline
value class AzureGrantType(val value: String)

@JvmInline
value class AzureWellKnownUrl(val value: String)

@JvmInline
value class AzureTokenEndpoint(val value: String)

@JvmInline
value class AzureApplicationId(val value: String)

@JvmInline
value class AzureApplicationSecret(val value: String)

data class AzureAuth(
    val azureAd: AzureAd,
    val azureHttpProxy: AzureHttpProxy,
    val azureAdAuth: AzureAdAuth,
    val azureGrantType: AzureGrantType,
    val azureWellKnownUrl: AzureWellKnownUrl,
    val azureTokenEndpoint: AzureTokenEndpoint,
    val azureAppClientId: AzureApplicationId,
    val azureAppClientSecret: AzureApplicationSecret
)
