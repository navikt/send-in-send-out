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
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import no.nav.emottak.melding.model.FrikortsporringRequest
import no.nav.emottak.melding.model.FrikortsporringResponse
import no.nav.emottak.utils.environment.getEnvVar
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI

val URL_FRIKORT_BASE = getEnvVar("URL_FRIKORT_REPO", "http://frikorttjenester.teamfrikort/api/ekstern/frikortsporringer")
val URL_FRIKORT_HAR_BORGER_FRIKORT = getEnvVar("URL_FRIKORT_HARBORGER", "$URL_FRIKORT_BASE/harborgerfrikort")
val URL_FRIKORT_HAR_BORGER_EGENANDELFRITAK = getEnvVar("URL_FRIKORT_HARBORGER", "$URL_FRIKORT_BASE/harborgeregenandelfritak")

const val AZURE_AD_AUTH = "AZURE_AD"

val LENIENT_JSON_PARSER = Json {
    isLenient = true
}

val frikortHttpClient = getFrikortRepoAuthenticatedClient()

val FRIKORT_REPO_SCOPE = getEnvVar(
    "FRIKORT_REPO_SCOPE",
    "api://" + getEnvVar("NAIS_CLUSTER_NAME", "dev-fss") +
        ".teamfrikort.frikorttjenester/.default"
)

suspend fun getFrikortRepoToken(): BearerTokens {
    val requestBody =
        "client_id=" + getEnvVar("AZURE_APP_CLIENT_ID", "ebms-send-in") +
            "&client_secret=" + getEnvVar("AZURE_APP_CLIENT_SECRET", "dummysecret") +
            "&scope=" + FRIKORT_REPO_SCOPE +
            "&grant_type=client_credentials"

    return HttpClient(CIO) {
        engine {
            val httpProxyUrl = getEnvVar("HTTP_PROXY", "")
            if (httpProxyUrl.isNotBlank()) {
                proxy = Proxy(
                    Proxy.Type.HTTP,
                    InetSocketAddress(URI.create(httpProxyUrl).host, URI.create(httpProxyUrl).port)
                )
            }
        }
    }.post(
        getEnvVar(
            "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT",
            "http://localhost:3344/$AZURE_AD_AUTH/token"
        )
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

fun getFrikortRepoAuthenticatedClient(): HttpClient {
    return HttpClient(CIO) {
        install(HttpTimeout) {
            this.requestTimeoutMillis = 60000
        }
        install(ContentNegotiation) {
            json()
        }
        installFrikortRepoAuthentication()
    }
}

fun HttpClientConfig<*>.installFrikortRepoAuthentication() {
    install(Auth) {
        bearer {
            refreshTokens {
                getFrikortRepoToken()
            }
            sendWithoutRequest {
                true
            }
        }
    }
}

suspend fun postHarBorgerFrikort(frikortsporringRequest: FrikortsporringRequest): FrikortsporringResponse {
    val httpResponse = runCatching {
        frikortHttpClient.post(URL_FRIKORT_HAR_BORGER_FRIKORT) {
            setBody(frikortsporringRequest)
        }
    }.onFailure { throwable ->
        throw throwable
    }.getOrThrow()
    return httpResponse.body<FrikortsporringResponse>()
}

suspend fun postHarBorgerEgenandelfritak(frikortsporringRequest: FrikortsporringRequest): FrikortsporringResponse {
    val httpResponse = runCatching {
        frikortHttpClient.post(URL_FRIKORT_HAR_BORGER_EGENANDELFRITAK) {
            setBody(frikortsporringRequest)
        }
    }.onFailure { throwable ->
        throw throwable
    }.getOrThrow()
    return httpResponse.body<FrikortsporringResponse>()
}
