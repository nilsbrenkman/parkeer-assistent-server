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

/**
 * Service responsible for topping up the user's parking wallet.
 *
 * Forwards a wallet top-up request to the upstream SSP (Self-Service Portal)
 * API through [Api] and returns the resulting transaction details (typically a
 * redirect/URL to complete payment). Emits metrics via [Metrics].
 */
object PaymentService {

    /**
     * Enumeration of service methods exposed by [PaymentService].
     *
     * Used by [Metrics] to tag log entries and counters with a consistent
     * service/method pair.
     */
    enum class Method : ServiceMethod {
        /** Initiate a wallet top-up transaction. */
        Payment,
        ;

        override fun service(): String {
            return "Payment"
        }

        override fun method(): String {
            return name
        }
    }

    /**
     * Initiates a wallet top-up transaction.
     *
     * The request body is parsed as a [PaymentRequest] and forwarded to the
     * upstream `wallet_transaction/new` endpoint. Any upstream status above
     * `299` is treated as a failure.
     *
     * @param call the incoming Ktor application call carrying the
     *             [PaymentRequest] body.
     * @return the [PaymentResponse] returned by the upstream API.
     * @throws Exception if the upstream call returns a non-success status.
     */
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
