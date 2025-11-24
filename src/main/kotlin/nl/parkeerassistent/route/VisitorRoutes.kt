package nl.parkeerassistent.route

import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import nl.parkeerassistent.RouterGroup
import nl.parkeerassistent.service.VisitorService

val visitorRoutes: RouterGroup = {

    route("/visitor") {
        get {
            call.respond(VisitorService.get(call))
        }
        post {
            call.respond(VisitorService.add(call))
        }
        delete("/{id}") {
            call.respond(VisitorService.delete(call))
        }
    }

}