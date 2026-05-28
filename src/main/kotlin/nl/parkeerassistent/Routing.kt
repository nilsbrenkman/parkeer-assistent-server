package nl.parkeerassistent

import io.ktor.server.application.Application
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import nl.parkeerassistent.mock.mock
import nl.parkeerassistent.mock.mockRouting
import nl.parkeerassistent.route.geoRoutes
import nl.parkeerassistent.route.loginRoutes
import nl.parkeerassistent.route.parkingRoutes
import nl.parkeerassistent.route.paymentRoutes
import nl.parkeerassistent.route.userRoutes
import nl.parkeerassistent.route.visitorRoutes

fun Application.configureRouting() {
    routing {
        mock {
            mockRouting()
        }
        rateLimit {
            loginRoutes()
            userRoutes()
            parkingRoutes()
            visitorRoutes()
            paymentRoutes()
            geoRoutes()
        }
        post("/") {
            call.respondRedirect("/", false)
        }
        staticResources("/.well-known", "well-known")
        staticResources("/", "static")
    }
}

typealias RouterGroup = Route.() -> Unit
