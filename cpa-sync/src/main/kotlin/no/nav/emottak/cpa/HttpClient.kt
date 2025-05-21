package no.nav.emottak.cpa

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
import no.nav.emottak.utils.environment.getEnvVar
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI

val URL_CPA_REPO_BASE = getEnvVar("URL_CPA_REPO", "http://cpa-repo.team-emottak.svc.nais.local")
val URL_CPA_REPO_PUT = "$URL_CPA_REPO_BASE/cpa".also { log.info("CPA REPO PUT URL: [$it]") }
val URL_CPA_REPO_DELETE = "$URL_CPA_REPO_BASE/cpa/delete"
val URL_CPA_REPO_TIMESTAMPS = "$URL_CPA_REPO_BASE/cpa/timestamps"

const val AZURE_AD_AUTH = "AZURE_AD"

val LENIENT_JSON_PARSER = Json {
    isLenient = true
}

val CPA_REPO_SCOPE = getEnvVar(
    "CPA_REPO_SCOPE",
    "api://" + getEnvVar("NAIS_CLUSTER_NAME", "dev-fss") +
        ".team-emottak.cpa-repo/.default"
)

suspend fun getCpaRepoToken(): BearerTokens {
    val requestBody =
        "client_id=" + getEnvVar("AZURE_APP_CLIENT_ID", "cpa-repo") +
            "&client_secret=" + getEnvVar("AZURE_APP_CLIENT_SECRET", "dummysecret") +
            "&scope=" + CPA_REPO_SCOPE +
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

fun getCpaRepoAuthenticatedClient(): HttpClient {
    return HttpClient(CIO) {
        install(HttpTimeout) {
            this.requestTimeoutMillis = 60000
        }
        install(ContentNegotiation) {
            json()
        }
        installCpaRepoAuthentication()
    }
}

fun HttpClientConfig<*>.installCpaRepoAuthentication() {
    install(Auth) {
        bearer {
            refreshTokens { // FIXME ingen forhold til expires-in...
                getCpaRepoToken()
            }
            sendWithoutRequest {
                true
            }
        }
    }
}

suspend fun HttpClient.getCPATimestamps() =
    Json.decodeFromString<Map<String, String>>(
        this.get(URL_CPA_REPO_TIMESTAMPS).bodyAsText()
    )

suspend fun HttpClient.putCPAinCPARepo(cpaFile: String, lastModified: String) =
    this.post(URL_CPA_REPO_PUT) {
        io.ktor.http.headers {
            header("updated_date", lastModified)
        }
        setBody(cpaFile)
    }

suspend fun HttpClient.deleteCPAinCPARepo(cpaId: String) = this.delete("$URL_CPA_REPO_DELETE/$cpaId")
