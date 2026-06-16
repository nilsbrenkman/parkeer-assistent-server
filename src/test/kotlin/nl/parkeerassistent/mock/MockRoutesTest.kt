package nl.parkeerassistent.mock

import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.cookie
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.setCookie
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import nl.parkeerassistent.model.AddVisitorRequest
import nl.parkeerassistent.model.BalanceResponse
import nl.parkeerassistent.model.LoginRequest
import nl.parkeerassistent.model.ParkingResponse
import nl.parkeerassistent.model.PaymentRequest
import nl.parkeerassistent.model.PaymentResponse
import nl.parkeerassistent.model.Response
import nl.parkeerassistent.model.UserResponse
import nl.parkeerassistent.model.VisitorResponse
import nl.parkeerassistent.module
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Exercises the full routing stack (module() -> MockRouteSelector -> mockRouting())
 * against the in-memory MockState, without any connection to the upstream Egis API.
 * All requests carry the `X-ParkeerAssistent-Mock: true` header so the mock selector
 * short-circuits the real routes.
 *
 * The `token` cookie carries the (compressed) mock state and is set by the server
 * without a path; rather than depend on the test client's cookie-path handling we
 * thread the token explicitly across calls — capturing the latest value after each
 * mutating response, since the state lives in the cookie itself.
 */
class MockRoutesTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun io.ktor.server.testing.ApplicationTestBuilder.mockClient() = createClient {
        install(DefaultRequest) {
            header("X-ParkeerAssistent-Mock", "true")
        }
    }

    private fun HttpResponse.token(): String? =
        setCookie().firstOrNull { it.name == "token" }?.value

    @Test
    fun `login succeeds with valid credentials`() = testApplication {
        application { module() }
        val client = mockClient()
        val response = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(LoginRequest("test", "1234")))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(json.decodeFromString<Response>(response.bodyAsText()).success)
    }

    @Test
    fun `login is case-insensitive on the username`() = testApplication {
        application { module() }
        val client = mockClient()
        val response = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(LoginRequest("TEST", "1234")))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(json.decodeFromString<Response>(response.bodyAsText()).success)
    }

    @Test
    fun `login fails with invalid credentials`() = testApplication {
        application { module() }
        val client = mockClient()
        val response = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(LoginRequest("test", "wrong")))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(!json.decodeFromString<Response>(response.bodyAsText()).success)
    }

    @Test
    fun `GET login reports not logged in without a session`() = testApplication {
        application { module() }
        val client = mockClient()
        val response = client.get("/login")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(!json.decodeFromString<Response>(response.bodyAsText()).success)
    }

    @Test
    fun `protected route returns 401 without a session`() = testApplication {
        application { module() }
        val client = mockClient()
        assertEquals(HttpStatusCode.Unauthorized, client.get("/parking").status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/visitor").status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/user").status)
    }

    @Test
    fun `full session flow over the mock state`() = testApplication {
        application { module() }

        // The token cookie carries the mock state; track its latest value and
        // attach it (RAW, as the server sets it) on every request.
        var token: String? = null
        val client = createClient {
            install(DefaultRequest) {
                header("X-ParkeerAssistent-Mock", "true")
                token?.let { cookie("token", it) }
            }
        }

        // Log in: this sets the token cookie carrying the seeded dummy state.
        val login = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(LoginRequest("test", "1234")))
        }
        assertEquals(HttpStatusCode.OK, login.status)
        token = login.token()
        assertNotNull(token)

        // The session is now established for subsequent calls.
        assertTrue(json.decodeFromString<Response>(client.get("/login").bodyAsText()).success)

        // /user returns a parseable balance and the seeded regime.
        val user = client.get("/user")
        assertEquals(HttpStatusCode.OK, user.status, "status=${user.status} body=${user.bodyAsText()}")
        val userResponse = json.decodeFromString<UserResponse>(user.bodyAsText())
        assertNotNull(userResponse.balance.toDoubleOrNull())
        assertTrue(userResponse.regime.days.isNotEmpty())

        // The dummy state seeds two visitors.
        val initial = json.decodeFromString<VisitorResponse>(client.get("/visitor").bodyAsText())
        assertEquals(2, initial.visitors.size)

        // Add a visitor and observe it in the list.
        val add = client.post("/visitor") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(AddVisitorRequest("33CCC3", "Klaas")))
        }
        assertEquals(HttpStatusCode.OK, add.status)
        token = add.token() ?: token
        val afterAdd = json.decodeFromString<VisitorResponse>(client.get("/visitor").bodyAsText())
        assertEquals(3, afterAdd.visitors.size)
        assertTrue(afterAdd.visitors.any { it.name == "Klaas" })

        // Delete a visitor and observe the list shrink.
        val toDelete = afterAdd.visitors.first().id
        val delete = client.delete("/visitor/$toDelete")
        assertEquals(HttpStatusCode.OK, delete.status)
        token = delete.token() ?: token
        val afterDelete = json.decodeFromString<VisitorResponse>(client.get("/visitor").bodyAsText())
        assertEquals(2, afterDelete.visitors.size)
        assertTrue(afterDelete.visitors.none { it.id == toDelete })

        // Parking sessions deserialize into active/scheduled buckets.
        val parking = json.decodeFromString<ParkingResponse>(client.get("/parking").bodyAsText())
        assertTrue(parking.active.isNotEmpty() || parking.scheduled.isNotEmpty())

        // Balance endpoint (nested under /user) returns a parseable amount.
        val balance = json.decodeFromString<BalanceResponse>(client.get("/user/balance").bodyAsText())
        assertNotNull(balance.balance.toDoubleOrNull())

        // Starting a payment yields a completion URL.
        val payment = client.post("/payment") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(PaymentRequest(2500, "SUCCESS", "NL")))
        }
        assertEquals(HttpStatusCode.OK, payment.status)
        val paymentResponse = json.decodeFromString<PaymentResponse>(payment.bodyAsText())
        assertTrue(paymentResponse.url.contains("completeMockPayment"))
    }
}
