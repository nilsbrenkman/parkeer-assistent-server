package nl.parkeerassistent.service

import io.ktor.client.call.body
import io.ktor.client.request.setBody
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import nl.parkeerassistent.Metrics
import nl.parkeerassistent.client.Api
import nl.parkeerassistent.model.PaymentRequest
import nl.parkeerassistent.model.PaymentResponse
import org.slf4j.event.Level

object PaymentService {

    enum class Method : ServiceMethod {
        Payment,
        ;

        override fun service(): String {
            return "Payment"
        }

        override fun method(): String {
            return name
        }
    }

    suspend fun payment(call: ApplicationCall): PaymentResponse {
        val request = call.receive<PaymentRequest>()
        val response = Api.post(call, "/v1/ssp/wallet_transaction/new") {
            setBody(request)
        }
        if (response.status.value > 299) {
            Metrics.logAndCount(call, Method.Payment, Level.ERROR, "ERROR")
            throw Exception("Payment failed")
        }
        Metrics.logAndCount(call, Method.Payment, Level.INFO, "SUCCESS")
        return response.body<PaymentResponse>()
    }

}
