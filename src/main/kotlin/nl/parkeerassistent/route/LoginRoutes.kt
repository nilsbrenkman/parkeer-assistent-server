package nl.parkeerassistent.route

import io.ktor.server.request.receive
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import nl.parkeerassistent.RouterGroup
import nl.parkeerassistent.client.respondWithSession
import nl.parkeerassistent.client.session
import nl.parkeerassistent.model.LoginRequest
import nl.parkeerassistent.service.LoginService

val loginRoutes: RouterGroup = {

    get("/login") {
        call.respondWithSession(LoginService.isLoggedIn(call.session()))
    }
    post("/login") {
        val request = call.receive<LoginRequest>()
        call.respondWithSession(LoginService.login(call.session(), request))
    }
    get("/logout") {
        call.respondWithSession(LoginService.logout(call.session()))
    }

}