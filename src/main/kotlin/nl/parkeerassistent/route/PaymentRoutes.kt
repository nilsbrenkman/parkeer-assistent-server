package nl.parkeerassistent.route

import io.ktor.server.request.receive
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import nl.parkeerassistent.RouterGroup
import nl.parkeerassistent.client.respondWithSession
import nl.parkeerassistent.client.session
import nl.parkeerassistent.model.CompleteRequest
import nl.parkeerassistent.model.PaymentRequest
import nl.parkeerassistent.service.PaymentService

val paymentRoutes: RouterGroup = {

    route("/payment") {
        get {
            call.respondWithSession(PaymentService.ideal(call.session()))
        }
        post {
            val request = call.receive<PaymentRequest>()
            call.respondWithSession(PaymentService.payment(call.session(), request))
        }
        post("/complete") {
            val request = call.receive<CompleteRequest>()
            call.respondWithSession(PaymentService.complete(call.session(), request))
        }
        get("/{transactionId}") {
            call.respondWithSession(PaymentService.status(call.session()))
        }
    }

}