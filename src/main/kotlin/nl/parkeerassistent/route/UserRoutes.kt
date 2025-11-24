package nl.parkeerassistent.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import nl.parkeerassistent.Metrics
import nl.parkeerassistent.RouterGroup
import nl.parkeerassistent.model.Response
import nl.parkeerassistent.service.UserService
import nl.parkeerassistent.service.UserService.Method
import org.slf4j.event.Level

val userRoutes: RouterGroup = {

    route("/user") {
        get {
            call.respond(UserService.get(call))
        }
        get("/balance") {
            call.respond(UserService.balance(call))
        }
        get("/regime/{id}") {
            try {
                call.respond(UserService.regime(call))
            } catch (_: Exception) {
                Metrics.logAndCount(call, Method.Regime, Level.WARN, "INVALID_PARKING_ZONE")
                call.respond(HttpStatusCode.BadRequest, Response(false, "Ongeldige parkeerzone"))
            }
        }
    }

}