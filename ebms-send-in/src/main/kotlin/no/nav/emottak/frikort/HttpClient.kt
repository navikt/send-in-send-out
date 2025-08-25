package no.nav.emottak.frikort

import com.nimbusds.jwt.SignedJWT
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import no.nav.emottak.log
import no.nav.emottak.utils.environment.getEnvVar
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI

val URL_FRIKORT_BASE = getEnvVar("URL_FRIKORT_REPO", "https://frikorttjenester.intern.dev.nav.no/api/ekstern/frikortsporringer")
val URL_FRIKORT_HARBORGER = getEnvVar("URL_FRIKORT_HARBORGER", "$URL_FRIKORT_BASE/frikortsporringer/harborgerfrikort")
val URL_FRIKORT_HARBORGER_EGENANDEL = getEnvVar("URL_FRIKORT_HARBORGER", "$URL_FRIKORT_BASE/frikortsporringer/harborgeregenandelfritak")
val URL_FRIKORT_REPO_PUT = "$URL_FRIKORT_BASE/cpa".also { log.info("FRIKORT REPO PUT URL: [$it]") }
val URL_FRIKORT_REPO_DELETE = "$URL_FRIKORT_BASE/cpa/delete"
val URL_FRIKORT_REPO_TIMESTAMPS = "$URL_FRIKORT_BASE/cpa/timestamps"

const val AZURE_AD_AUTH = "AZURE_AD"

val LENIENT_JSON_PARSER = Json {
    isLenient = true
}

val FRIKORT_REPO_SCOPE = getEnvVar(
    "FRIKORT_REPO_SCOPE",
    "api://" + getEnvVar("NAIS_CLUSTER_NAME", "dev-fss") +
        ".team-emottak.cpa-repo/.default"
)

suspend fun getFrikortRepoToken(): BearerTokens {
    val requestBody =
        "client_id=" + getEnvVar("AZURE_APP_CLIENT_ID", "cpa-repo") +
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
            BearerTokens(parsedJwt.serialize(), "dummy") // FIXME dumt at den ikke tillater null for refresh token. Tyder på at den ikke bør brukes. Kanskje best å skrive egen handler
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
            refreshTokens { // FIXME ingen forhold til expires-in...
                getFrikortRepoToken()
            }
            sendWithoutRequest {
                true
            }
        }
    }
}

suspend fun HttpClient.getFrikortTimestamps() =
    Json.decodeFromString<Map<String, String>>(
        this.get(URL_FRIKORT_REPO_TIMESTAMPS).bodyAsText()
    )

suspend fun HttpClient.putFrikortinFrikortRepo(cpaFile: String, lastModified: String) =
    this.post(URL_FRIKORT_REPO_PUT) {
        io.ktor.http.headers {
            header("updated_date", lastModified)
        }
        setBody(cpaFile)
    }

suspend fun HttpClient.deleteFrikortinFrikortRepo(cpaId: String) = this.delete("$URL_FRIKORT_REPO_DELETE/$cpaId")
