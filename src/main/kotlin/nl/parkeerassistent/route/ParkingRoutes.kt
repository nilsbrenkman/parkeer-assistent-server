package nl.parkeerassistent.route


import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import nl.parkeerassistent.RouterGroup
import nl.parkeerassistent.service.ParkingService

val parkingRoutes: RouterGroup = {

    route("/parking") {
        get {
            call.respond(ParkingService.get(call))
        }
        post {
            call.respond(ParkingService.start(call))
        }
        delete("/{id}") {
            call.respond(ParkingService.stop(call))
        }
        get("/history") {
            call.respond(ParkingService.history(call))
        }
    }

}

