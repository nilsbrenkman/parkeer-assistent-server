package nl.parkeerassistent.mock


import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingResolveContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import nl.parkeerassistent.client.ensureData
import nl.parkeerassistent.model.AddParkingRequest
import nl.parkeerassistent.model.AddVisitorRequest
import nl.parkeerassistent.model.BalanceResponse
import nl.parkeerassistent.model.CompleteRequest
import nl.parkeerassistent.model.HistoryResponse
import nl.parkeerassistent.model.IdealResponse
import nl.parkeerassistent.model.Issuer
import nl.parkeerassistent.model.LoginRequest
import nl.parkeerassistent.model.ParkingResponse
import nl.parkeerassistent.model.PaymentRequest
import nl.parkeerassistent.model.RegimeResponse
import nl.parkeerassistent.model.Response
import nl.parkeerassistent.model.UserResponse
import nl.parkeerassistent.model.VisitorResponse
import nl.parkeerassistent.util.DateUtil
import java.util.Optional
import javax.naming.NoPermissionException

fun Route.mockRouting() {
    get("/login") {
        call.respond(Response(MockStateContainer.mock().user.loggedIn, ""), )
    }
    post("/login") {
        val request = call.receive<LoginRequest>()
        if (request.username.lowercase() == "test" && request.password == "1234") {
            MockStateContainer.mock().user.loggedIn = true
        } else if (request.username.lowercase() == "reset") {
            MockStateContainer.reset()
        } else {
            call.response.status(HttpStatusCode.Unauthorized)
            throw NoPermissionException()
        }
        call.respond(Response(MockStateContainer.mock().user.loggedIn, ""))
    }
    get("/logout") {
        MockStateContainer.mock().user.loggedIn = false
        call.respond(Response(MockStateContainer.mock().user.loggedIn.not(), ""))
    }
    route("/user") {
        get {
            preCheck(call)
            val user = MockStateContainer.mock().user
            call.respond(
                UserResponse(
                    "%.2f".format(MockStateContainer.mock().balance),
                    user.hourRate,
                    user.regimeStart,
                    user.regimeEnd,
                    user.regime
                )
            )
        }
        get("/balance") {
            preCheck(call)
            call.respond(BalanceResponse("%.2f".format(MockStateContainer.mock().balance)))
        }
        get("/regime/{date}") {
            preCheck(call)
            val date = ensureData(call.parameters["date"], "date")
            val (start, end) = MockStateContainer.mock().user.regimeForDate(DateUtil.date.parse(date))
            call.respond(RegimeResponse(start, end))
        }
    }
    route("/parking") {
        get {
            preCheck(call)
            call.respond(ParkingResponse(MockStateContainer.mock().active, MockStateContainer.mock().scheduled))
        }
        post {
            val request = call.receive<AddParkingRequest>()
            preCheck(call)

            MockStateContainer.mock().startParking(request)
            call.respond(Response(true, ""))
        }
        delete("/{id}") {
            val id = ensureData(call.parameters["id"]?.toLong(), "parking id")
            preCheck(call)

            MockStateContainer.mock().stopParking(id)
            call.respond(Response(true, ""))
        }
        get("/history") {
            preCheck(call)
            call.respond(HistoryResponse(MockStateContainer.mock().history))
        }
    }
    route("/visitor") {
        get {
            preCheck(call)
            call.respond(VisitorResponse(MockStateContainer.mock().visitors))
        }
        post {
            val request = call.receive<AddVisitorRequest>()
            preCheck(call)
            MockStateContainer.mock().addVisitor(request.name, request.license)
            call.respond(Response(true, ""))
        }
        delete("/{id}") {
            val id = ensureData(call.parameters["id"]?.toInt(), "visitor id")
            preCheck(call)

            MockStateContainer.mock().deleteVisitor(id)
            call.respond(Response(true, ""))
        }
    }
    route("/payment") {
        get {
            preCheck(call)
            call.respond(
                IdealResponse(
                    listOf("5,00", "10,00", "15,00", "20,00", "30,00", "40,00", "50,00", "100,00"),
                    listOf(
                        Issuer("SUCCESS", "Success"),
                        Issuer("PENDING", "Pending"),
                        Issuer("PENDING10", "Pending 10s"),
                        Issuer("ERROR", "Error"))
                )
            )
        }
        post {
            val request = call.receive<PaymentRequest>()
            preCheck(call)

            call.respond(MockStateContainer.mock().startPayment(request))
        }
        post("/complete") {
            val request = call.receive<CompleteRequest>()
            preCheck(call)
            val status = MockStateContainer.mock().checkPayment(request.transactionId)
            call.respond(Response(status.status == "success", ""))
        }
        get("/{id}") {
            val id = ensureData(call.parameters["id"], "payment id")
            preCheck(call)
            call.respond(MockStateContainer.mock().checkPayment(id))
        }
    }

}

fun preCheck(call: ApplicationCall) {
    if (MockStateContainer.mock().user.loggedIn.not()) {
        call.response.status(HttpStatusCode.Unauthorized)
        throw NoPermissionException()
    }
}

fun Route.mock(build: Route.() -> Unit): Route {
    val selector = MockRouteSelector()
    return createChild(selector).apply(build)
}

class MockRouteSelector : RouteSelector() {

    private val mockBuilds: List<String> = Optional
        .ofNullable(System.getenv("MOCK_BUILDS"))
        .or { Optional.of("") }
        .get().split(",")

    override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation {
        val mockHeader = context.call.request.headers["X-ParkeerAssistent-Mock"]
        if ("true".equals(mockHeader, ignoreCase = true)) {
            return RouteSelectorEvaluation.Constant
        }
        if (mockBuilds.isNotEmpty()) {
            val buildHeader = context.call.request.headers["X-ParkeerAssistent-Build"]
            if (mockBuilds.contains(buildHeader)) {
                return RouteSelectorEvaluation.Constant
            }
        }
        return RouteSelectorEvaluation.Failed
    }

}