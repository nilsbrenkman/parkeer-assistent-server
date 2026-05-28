package nl.parkeerassistent.mock


import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.MissingRequestParameterException
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
import nl.parkeerassistent.model.AddParkingRequest
import nl.parkeerassistent.model.AddVisitorRequest
import nl.parkeerassistent.model.BalanceResponse
import nl.parkeerassistent.model.HistoryResponse
import nl.parkeerassistent.model.LoginRequest
import nl.parkeerassistent.model.ParkingResponse
import nl.parkeerassistent.model.PaymentRequest
import nl.parkeerassistent.model.Response
import nl.parkeerassistent.model.UserResponse
import nl.parkeerassistent.model.VisitorResponse
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
            call.respond(Response(false, "Wrong username or password"))
            return@post
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
                    balance = "%.2f".format(MockStateContainer.mock().balance),
                    hourRate = 2.34,
                    productId = 0,
                    zoneId = 1,
                    parkingMeterId = 55105,
                    regime = user.regime,
                )
            )
        }
        get("/balance") {
            preCheck(call)
            call.respond(BalanceResponse("%.2f".format(MockStateContainer.mock().balance)))
        }
        get("/regime/{id}") {
            preCheck(call)
            val date = call.parameters["id"]?.toLong() ?: throw Exception("id is required")
//            val (start, end) = MockStateContainer.mock().user.regimeForDate(DateUtil.date.parse(date))
//            call.respond(RegimeResponse(start, end))
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
            val id = call.parameters["id"]?.toLong() ?: throw MissingRequestParameterException("id is required")
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
            val id = call.parameters["id"]?.toLong() ?: throw MissingRequestParameterException("id is required")
            preCheck(call)

            MockStateContainer.mock().deleteVisitor(id)
            call.respond(Response(true, ""))
        }
    }
    route("/payment") {
        post {
            val request = call.receive<PaymentRequest>()
            preCheck(call)

            call.respond(MockStateContainer.mock().startPayment(request))
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