package nl.parkeerassistent.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import nl.parkeerassistent.Metrics
import nl.parkeerassistent.RouterGroup
import nl.parkeerassistent.service.GeoService
import nl.parkeerassistent.service.Point
import org.slf4j.event.Level

val geoRoutes: RouterGroup = {

    route("/geo") {

        get("/parking-meters/nearby") {
            val lat = call.request.queryParameters["lat"]?.toDoubleOrNull() ?: throw BadRequestException("lat")
            val lon = call.request.queryParameters["lon"]?.toDoubleOrNull() ?: throw BadRequestException("lon")
            val n = call.request.queryParameters["n"]?.toIntOrNull() ?: 25
            Metrics.logAndCount(call, GeoService.Method.ParkingMeters, Level.INFO, "NEARBY")
            call.respond(GeoService.getParkingMetersNearby(Point.from(lat, lon), n))
        }

        get("/parking-meters/in-region") {
            val lat = call.request.queryParameters["lat"]?.toDoubleOrNull() ?: throw BadRequestException("lat")
            val lon = call.request.queryParameters["lon"]?.toDoubleOrNull() ?: throw BadRequestException("lon")
            Metrics.logAndCount(call, GeoService.Method.ParkingMeters, Level.INFO, "REGION")
            call.respond(GeoService.getParkingMetersInRegion(Point.from(lat, lon), Point.from(lat, lon)))
        }

        get("/parking-meters/{id}") {
            try {
                call.respond(GeoService.details(call))
            } catch (_: NotFoundException) {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }

}