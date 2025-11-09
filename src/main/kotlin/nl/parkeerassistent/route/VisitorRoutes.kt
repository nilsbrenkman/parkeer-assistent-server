package nl.parkeerassistent.route

import io.ktor.server.request.receive
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import nl.parkeerassistent.RouterGroup
import nl.parkeerassistent.client.respondWithSession
import nl.parkeerassistent.client.session
import nl.parkeerassistent.model.AddVisitorRequest
import nl.parkeerassistent.service.VisitorService

val visitorRoutes: RouterGroup = {

    route("/visitor") {
        get {
            call.respondWithSession(VisitorService.get(call.session()))
        }
        post {
            val request = call.receive<AddVisitorRequest>()
            call.respondWithSession(VisitorService.add(call.session(), request))
        }
        delete("/{visitorId}") {
            call.respondWithSession(VisitorService.delete(call.session()))
        }
    }

}