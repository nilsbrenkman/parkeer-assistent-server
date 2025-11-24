package nl.parkeerassistent.route

import io.ktor.server.routing.get
import io.ktor.server.routing.post
import nl.parkeerassistent.RouterGroup
import nl.parkeerassistent.service.LoginService

val loginRoutes: RouterGroup = {

    get("/login") {
        LoginService.isLoggedIn(call)
    }
    post("/login") {
        LoginService.login(call)
    }
    get("/logout") {
        LoginService.logout(call)
    }

}