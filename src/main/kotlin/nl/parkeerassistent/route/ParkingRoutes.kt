package nl.parkeerassistent.route

import io.ktor.server.request.receive
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import nl.parkeerassistent.RouterGroup
import nl.parkeerassistent.client.respondWithSession
import nl.parkeerassistent.client.session
import nl.parkeerassistent.model.AddParkingRequest
import nl.parkeerassistent.service.ParkingService

val parkingRoutes: RouterGroup = {

    route("/parking") {
        get {
            call.respondWithSession(ParkingService.get(call.session()))
        }
        post {
            val request = call.receive<AddParkingRequest>()
            call.respondWithSession(ParkingService.start(call.session(), request))
        }
        delete("/{id}") {
            call.respondWithSession(ParkingService.stop(call.session()))
        }
        get("/history") {
            call.respondWithSession(ParkingService.history(call.session()))
        }
    }

}

