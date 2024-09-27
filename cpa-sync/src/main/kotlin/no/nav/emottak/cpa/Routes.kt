package no.nav.emottak.cpa

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.emottak.cpa.nfs.NFSConnector
import org.slf4j.LoggerFactory

fun Route.cpaSync(): Route = get("/cpa-sync") {
    val log = LoggerFactory.getLogger("no.nav.emottak.smtp.sftp")
    val cpaSyncService = CpaSyncService(getCpaRepoAuthenticatedClient(), NFSConnector())

    withContext(Dispatchers.IO) {
        log.info("Starting CPA sync")
        val (result, duration) = measureTimeSuspended {
            runCatching {
                cpaSyncService.sync()
            }
        }
        result.onSuccess {
            log.info("CPA sync completed in $duration")
            call.respond(HttpStatusCode.OK, "CPA sync complete")
        }.onFailure {
            call.respond(HttpStatusCode.InternalServerError, "Something went wrong")
        }
    }

    call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
}

fun Route.testAzureAuthToCpaRepo(): Route = get("/testCpaRepoConnection") {
    val cpaRepoClient = getCpaRepoAuthenticatedClient()
    call.respond(
        cpaRepoClient.get("$URL_CPA_REPO_BASE/whoami").bodyAsText()
    )
}

var BUG_ENCOUNTERED_CPA_REPO_TIMEOUT = false
fun Routing.registerHealthEndpoints() {
    get("/internal/health/liveness") {
        if (BUG_ENCOUNTERED_CPA_REPO_TIMEOUT) { // TODO : årsak ukjent, cpa-repo/timestamps endepunkt timer ut etter en stund
            call.respond(HttpStatusCode.ServiceUnavailable, "Restart me X_X")
        } else {
            call.respondText("I'm alive! :)")
        }
    }
    get("/internal/health/readiness") {
        call.respondText("I'm ready! :)")
    }
}
