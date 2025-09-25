package no.nav.emottak.frikort.rest

import com.nimbusds.jwt.SignedJWT
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import no.nav.emottak.config.AppScope
import no.nav.emottak.config.AzureAuth
import no.nav.emottak.config.config
import no.nav.emottak.melding.model.FrikortsporringRequest
import no.nav.emottak.melding.model.FrikortsporringResponse
import no.nav.emottak.util.LogLevel
import no.nav.emottak.util.asJson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.RuntimeException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import kotlin.also

val log: Logger = LoggerFactory.getLogger("no.nav.emottak.frikort.rest.FrikortHttpClient")

val LENIENT_JSON_PARSER = Json {
    isLenient = true
}

val frikortHttpClient = httpClientAuthenticatedForFrikortTjenester()

suspend fun getFrikorttjenesterToken(azureAuth: AzureAuth, scope: AppScope): BearerTokens {
    val requestBody =
        "client_id=${azureAuth.azureAppClientId.value}" +
            "&client_secret=${azureAuth.azureAppClientSecret.value}" +
            "&scope=${scope.value}" +
            "&grant_type=${azureAuth.azureGrantType.value}"

    return HttpClient(CIO) {
        engine {
            val httpProxyUrl = azureAuth.azureHttpProxy.value
            if (httpProxyUrl.isNotBlank()) {
                proxy = Proxy(
                    Proxy.Type.HTTP,
                    InetSocketAddress(URI.create(httpProxyUrl).host, URI.create(httpProxyUrl).port)
                )
            }
        }
    }.post(
        azureAuth.azureTokenEndpoint.value
    ) {
        headers {
            header("Content-Type", "application/x-www-form-urlencoded")
        }
        setBody(requestBody)
    }.bodyAsText()
        .let { tokenResponseString ->
            SignedJWT.parse(
                LENIENT_JSON_PARSER.decodeFromString<Map<String, String>>(tokenResponseString)["access_token"] as String
            )
        }
        .let { parsedJwt ->
            BearerTokens(parsedJwt.serialize(), "dummy")
        }
}

fun httpClientAuthenticatedForFrikortTjenester(): HttpClient {
    return HttpClient(CIO) {
        install(HttpTimeout) {
            this.requestTimeoutMillis = 60000
        }
        install(ContentNegotiation) {
            json()
        }
        installFrikorttjenesterAuthentication()
    }
}

fun HttpClientConfig<*>.installFrikorttjenesterAuthentication() {
    install(Auth) {
        bearer {
            refreshTokens {
                getFrikorttjenesterToken(config().azureAuth, config().frikorttjenester.scope)
            }
            sendWithoutRequest {
                true
            }
        }
    }
}

suspend fun postHarBorgerFrikort(frikortsporringRequest: FrikortsporringRequest): FrikortsporringResponse {
    log.asJson(
        LogLevel.DEBUG,
        message = "HarBorgerFrikort REST Json request",
        obj = frikortsporringRequest,
        serializer = FrikortsporringRequest.serializer()
    )
    val httpResponse = frikortHttpClient.post(config().frikorttjenester.harBorgerFrikortEndpoint.value) {
        setBody(frikortsporringRequest)
        contentType(ContentType.Application.Json)
    }
    return when (httpResponse.status) {
        HttpStatusCode.OK -> httpResponse.body<FrikortsporringResponse>()
        else -> {
            log.error("Frikort response: ${httpResponse.status} - ${httpResponse.bodyAsText()}")
            throw RuntimeException(httpResponse.bodyAsText())
        }
    }.also {
        log.asJson(
            LogLevel.DEBUG,
            message = "HarBorgerFrikort REST Json response",
            obj = it,
            serializer = FrikortsporringResponse.serializer()
        )
    }
}

suspend fun postHarBorgerEgenandelfritak(frikortsporringRequest: FrikortsporringRequest): FrikortsporringResponse {
    log.asJson(
        LogLevel.DEBUG,
        message = "HarBorgerEgenandelfritak REST Json request",
        obj = frikortsporringRequest,
        serializer = FrikortsporringRequest.serializer()
    )
    val httpResponse = frikortHttpClient.post(config().frikorttjenester.harBorgerEgenandelFritakEndpoint.value) {
        setBody(frikortsporringRequest)
        contentType(ContentType.Application.Json)
    }
    return when (httpResponse.status) {
        HttpStatusCode.OK -> httpResponse.body<FrikortsporringResponse>()
        else -> {
            log.error("Frikort response: ${httpResponse.status} - ${httpResponse.bodyAsText()}")
            throw RuntimeException(httpResponse.bodyAsText())
        }
    }.also {
        log.asJson(
            LogLevel.DEBUG,
            message = "HarBorgerEgenandelfritak REST Json response",
            obj = it,
            serializer = FrikortsporringResponse.serializer()
        )
    }
}
