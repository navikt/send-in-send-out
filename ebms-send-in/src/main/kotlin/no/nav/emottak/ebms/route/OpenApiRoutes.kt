package no.nav.emottak.ebms.route

import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route

fun Routing.openApiRoutes() {
    route("api.json") {
        openApi()
    }

    route("swagger") {
        swaggerUI("/api.json")
    }
}
