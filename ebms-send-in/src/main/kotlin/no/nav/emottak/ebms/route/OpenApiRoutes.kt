package no.nav.emottak.ebms.route

import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.server.routing.Route
import io.ktor.server.routing.route

fun Route.openApiRoutes() {
    route("api.json") {
        openApi()
    }

    route("swagger") {
        swaggerUI("/api.json")
    }
}
