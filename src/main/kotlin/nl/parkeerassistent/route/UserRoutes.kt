package nl.parkeerassistent.route

import io.ktor.server.routing.get
import io.ktor.server.routing.route
import nl.parkeerassistent.RouterGroup
import nl.parkeerassistent.client.respondWithSession
import nl.parkeerassistent.client.session
import nl.parkeerassistent.service.UserService

val userRoutes: RouterGroup = {

    route("/user") {
        get {
            call.respondWithSession(UserService.get(call.session()))
        }
        get("/balance") {
            call.respondWithSession(UserService.balance(call.session()))
        }
        get("/regime/{date}") {
            call.respondWithSession(UserService.regime(call.session()))
        }
    }

}