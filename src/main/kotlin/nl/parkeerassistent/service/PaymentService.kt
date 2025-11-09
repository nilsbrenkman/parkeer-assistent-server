package nl.parkeerassistent.service

import io.ktor.client.call.body
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import nl.parkeerassistent.Metrics
import nl.parkeerassistent.client.CloudApi
import nl.parkeerassistent.client.Session
import nl.parkeerassistent.client.ensureData
import nl.parkeerassistent.external.Balance
import nl.parkeerassistent.external.Order
import nl.parkeerassistent.external.Payment
import nl.parkeerassistent.external.PaymentOrder
import nl.parkeerassistent.external.Redirect
import nl.parkeerassistent.model.CompleteRequest
import nl.parkeerassistent.model.IdealResponse
import nl.parkeerassistent.model.Issuer
import nl.parkeerassistent.model.PaymentRequest
import nl.parkeerassistent.model.PaymentResponse
import nl.parkeerassistent.model.Response
import nl.parkeerassistent.model.StatusResponse
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

object PaymentService {

    private val log = LoggerFactory.getLogger(PaymentService::class.java)

    enum class Method : ServiceMethod {
        Ideal,
        Payment,
        Complete,
        Status,
        ;

        override fun service(): String {
            return "Payment"
        }

        override fun method(): String {
            return name
        }
    }

    val amounts = arrayListOf("2.50", "5.00", "10.00", "15.00", "20.00", "30.00", "40.00", "50.00", "100.00")

    fun ideal(session: Session): IdealResponse {

        val issuers = ArrayList<Issuer>()
        issuers.addAll(IdealBanks.entries.map { i -> Issuer(i.name, i.displayName) })

        Metrics.logAndCount(session.call, Method.Ideal, Level.INFO, "SUCCESS")
        return IdealResponse(amounts, issuers)
    }

    suspend fun payment(session: Session, request: PaymentRequest): PaymentResponse {

        val payment = Payment(
            Balance(request.amount.toDouble(), "EUR"),
            Redirect("https://parkeerassistent.nl/completePayment")
        )

        val order: PaymentOrder = CloudApi.post(session, "v1/orders") {
            setBody(payment)
        }.body()

        val rabo = Jsoup.connect(order.redirectUrl).method(Connection.Method.GET).execute().parse()
        val issuer = rabo.select("a#issuer-" + request.issuerId)
        if (issuer.size != 1) {
            Metrics.logAndCount(session.call, Method.Payment, Level.WARN, "ISSUER_NOT_FOUND")
            return PaymentResponse(order.redirectUrl, order.frontendId.toString())
        }
        val link = issuer[0].attr("href")

        return PaymentResponse(link, order.frontendId.toString())
    }

    suspend fun complete(session: Session, request: CompleteRequest): Response {
        try {
            val order: String = session.client.get(Session.getMainUrl("api/orders?transactionType=topUpBalance&${request.data}")).body()
            log.debug(order)
        } catch(e: RedirectResponseException) {
            if (e.response.status.value == 302) {
                if (e.response.headers["Location"]?.startsWith("/top-up-balance/success") == true) {
                    if (CloudApi.waitForOrder(session, request.transactionId.toLong())) {
                        Metrics.logAndCount(session.call, Method.Complete, Level.INFO, "SUCCESS")
                        return Response(true)
                    }
                    Metrics.logAndCount(session.call, Method.Complete, Level.INFO, "PENDING")
                    return Response(false)
                }
                Metrics.logAndCount(session.call, Method.Complete, Level.INFO, "FAILED")
                return Response(false)
            }
        }
        Metrics.logAndCount(session.call, Method.Complete, Level.INFO, "ERROR")
        return Response(false)
    }

    suspend fun status(session: Session): StatusResponse {
        val transactionId = ensureData(session.call.parameters["transactionId"], "transaction id")

        val order: Order = CloudApi.get(session, "v1/orders/$transactionId").body()

        when(order.orderStatus) {
            "Completed" -> {
                Metrics.logAndCount(session.call, Method.Status, Level.INFO, "SUCCESS")
                return StatusResponse("success")
            }
            "Processing" -> {
                Metrics.logAndCount(session.call, Method.Status, Level.INFO, "PENDING")
                return StatusResponse("pending")
            }
            else -> {
                Metrics.logAndCount(session.call, Method.Status, Level.INFO, "UNKNOWN")
                return StatusResponse("unknown")
            }
        }

    }

}

private enum class IdealBanks(val displayName: String) {
    ABNANL2A("ABN AMRO"),
    ASNBNL21("ASN Bank"),
    BUNQNL2A("bunq"),
    HANDNL2A("Handelsbanken"),
    INGBNL2A("ING"),
    KNABNL2H("Knab"),
    RABONL2U("Rabobank"),
    RBRBNL21("RegioBank"),
    SNSBNL2A("SNS"),
    TRIONL2U("Triodos Bank"),
    FVLBNL22("Van Lanschot"),
}