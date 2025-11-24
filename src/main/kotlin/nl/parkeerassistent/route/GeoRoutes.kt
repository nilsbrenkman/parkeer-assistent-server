package nl.parkeerassistent.route

import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import nl.parkeerassistent.RouterGroup
import nl.parkeerassistent.service.GeoService
import nl.parkeerassistent.service.Point

val geoRoutes: RouterGroup = {

    route("/geo") {

        get("/parking-meters/nearby") {
            val lat = call.request.queryParameters["lat"]?.toDoubleOrNull() ?: throw BadRequestException("lat")
            val lon = call.request.queryParameters["lon"]?.toDoubleOrNull() ?: throw BadRequestException("lon")
            val n = call.request.queryParameters["n"]?.toIntOrNull() ?: 25
            call.respond(GeoService.getParkingMetersNearby(Point.from(lat, lon), n))
        }

        get("/parking-meters/in-region") {
            val lat = call.request.queryParameters["lat"]?.toDoubleOrNull() ?: throw BadRequestException("lat")
            val lon = call.request.queryParameters["lon"]?.toDoubleOrNull() ?: throw BadRequestException("lon")
            call.respond(GeoService.getParkingMetersInRegion(Point.from(lat, lon), Point.from(lat, lon)))
        }
    }


}