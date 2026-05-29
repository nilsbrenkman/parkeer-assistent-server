package nl.parkeerassistent.mock


import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.MissingRequestParameterException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.RoutingResolveContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import nl.parkeerassistent.JSON
import nl.parkeerassistent.client.clearTokenCookie
import nl.parkeerassistent.model.AddParkingRequest
import nl.parkeerassistent.model.AddVisitorRequest
import nl.parkeerassistent.model.BalanceResponse
import nl.parkeerassistent.model.HistoryResponse
import nl.parkeerassistent.model.LoginRequest
import nl.parkeerassistent.model.ParkingResponse
import nl.parkeerassistent.model.PaymentRequest
import nl.parkeerassistent.model.RegimeResponse
import nl.parkeerassistent.model.Response
import nl.parkeerassistent.model.UserResponse
import nl.parkeerassistent.model.VisitorResponse
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.Optional
import java.util.zip.Deflater
import java.util.zip.Inflater

private const val TOKEN_COOKIE = "token"

fun Route.mockRouting() {
    get("/login") {
        val loggedIn = call.readMockState() != null
        call.respond(Response(loggedIn, if (loggedIn) "Je hebt een token" else "Je bent niet ingelogd"))
    }
    post("/login") {
        val request = call.receive<LoginRequest>()
        val username = request.username.lowercase()
        if (username == "test" && request.password == "1234") {
            call.saveMockState(MockState.dummy())
            call.respond(Response(true, "Success"))
        } else {
            call.respond(HttpStatusCode.Unauthorized, Response(false, "Gebruikersnaam en/of wachtwoord onjuist"))
        }
    }
    get("/logout") {
        call.clearTokenCookie()
        call.respond(Response(true, "Je bent uitgelogd"))
    }
    route("/user") {
        get {
            val state = requireState() ?: return@get
            call.respond(
                UserResponse(
                    balance = "%.2f".format(state.balance),
                    hourRate = 2.34,
                    productId = 0,
                    zoneId = 1,
                    parkingMeterId = 55105,
                    regime = MockState.regime,
                )
            )
        }
        get("/balance") {
            val state = requireState() ?: return@get
            call.respond(BalanceResponse("%.2f".format(state.balance)))
        }
        get("/regime/{id}") {
            requireState() ?: return@get
            val parkingMeterId = call.parameters["id"]?.toLongOrNull()
            if (parkingMeterId == null) {
                call.respond(HttpStatusCode.BadRequest, Response(false, "Ongeldige parkeerzone"))
                return@get
            }
            call.respond(RegimeResponse(hourRate = 2.34, zoneId = 1, regime = MockState.regime))
        }
    }
    route("/parking") {
        get {
            val state = requireState() ?: return@get
            call.respond(ParkingResponse(state.active, state.scheduled))
        }
        post {
            val state = requireState() ?: return@post
            val request = call.receive<AddParkingRequest>()
            state.startParking(request)
            call.saveMockState(state)
            call.respond(Response(true, "success"))
        }
        delete("/{id}") {
            val state = requireState() ?: return@delete
            val id = call.parameters["id"]?.toLong() ?: throw MissingRequestParameterException("id")
            state.stopParking(id)
            call.saveMockState(state)
            call.respond(Response(true, "success"))
        }
        get("/history") {
            val state = requireState() ?: return@get
            call.respond(HistoryResponse(state.history))
        }
    }
    route("/visitor") {
        get {
            val state = requireState() ?: return@get
            call.respond(VisitorResponse(state.visitorList))
        }
        post {
            val state = requireState() ?: return@post
            val request = call.receive<AddVisitorRequest>()
            state.addVisitor(request.name, request.license)
            call.saveMockState(state)
            call.respond(Response(true, "success"))
        }
        delete("/{id}") {
            val state = requireState() ?: return@delete
            val id = call.parameters["id"]?.toLong() ?: throw MissingRequestParameterException("id")
            state.deleteVisitor(id)
            call.saveMockState(state)
            call.respond(Response(true, "success"))
        }
    }
    route("/payment") {
        post {
            val state = requireState() ?: return@post
            val request = call.receive<PaymentRequest>()
            val response = state.startPayment(request)
            call.saveMockState(state)
            call.respond(response)
        }
    }
}

/**
 * Loads the mock state from the token cookie. When absent or invalid the request is
 * treated as unauthenticated: the token cookie is cleared and a 401 is returned,
 * matching the behaviour of the real backend on an expired/invalid token.
 */
private suspend fun RoutingContext.requireState(): MockState? {
    val state = call.readMockState()
    if (state == null) {
        call.clearTokenCookie()
        call.respond(HttpStatusCode.Unauthorized, "Not authorized")
    }
    return state
}

private fun ApplicationCall.readMockState(): MockState? {
    val raw = request.cookies[TOKEN_COOKIE, CookieEncoding.RAW] ?: return null
    return try {
        val json = inflate(Base64.getUrlDecoder().decode(raw)).toString(Charsets.UTF_8)
        JSON.decodeFromString<MockState>(json)
    } catch (e: Exception) {
        null
    }
}

private fun ApplicationCall.saveMockState(state: MockState) {
    val deflated = deflate(JSON.encodeToString(state).toByteArray(Charsets.UTF_8))
    val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(deflated)
    response.cookies.append(
        Cookie(
            name = TOKEN_COOKIE,
            value = encoded,
            encoding = CookieEncoding.RAW,
            httpOnly = true,
            secure = false,
        )
    )
}

private fun deflate(data: ByteArray): ByteArray {
    val deflater = Deflater(Deflater.BEST_COMPRESSION)
    deflater.setInput(data)
    deflater.finish()
    val output = ByteArrayOutputStream(data.size)
    val buffer = ByteArray(1024)
    while (!deflater.finished()) {
        output.write(buffer, 0, deflater.deflate(buffer))
    }
    deflater.end()
    return output.toByteArray()
}

private fun inflate(data: ByteArray): ByteArray {
    val inflater = Inflater()
    inflater.setInput(data)
    val output = ByteArrayOutputStream(data.size * 4)
    val buffer = ByteArray(1024)
    while (!inflater.finished()) {
        val count = inflater.inflate(buffer)
        if (count == 0 && inflater.needsInput()) break
        output.write(buffer, 0, count)
    }
    inflater.end()
    return output.toByteArray()
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
