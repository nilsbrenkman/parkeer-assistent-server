package nl.parkeerassistent.route

import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import nl.parkeerassistent.RouterGroup
import nl.parkeerassistent.service.PaymentService

val paymentRoutes: RouterGroup = {

    route("/payment") {
        post {
            call.respond(PaymentService.payment(call))
        }
    }

}