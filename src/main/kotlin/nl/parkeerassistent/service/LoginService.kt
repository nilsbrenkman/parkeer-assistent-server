package nl.parkeerassistent.service

import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import nl.parkeerassistent.Metrics
import nl.parkeerassistent.client.Api
import nl.parkeerassistent.client.clearTokenCookie
import nl.parkeerassistent.model.LoginRequest
import nl.parkeerassistent.model.LoginResponse
import nl.parkeerassistent.model.Response
import nl.parkeerassistent.service.LoginService.isLoggedIn
import nl.parkeerassistent.service.LoginService.login
import nl.parkeerassistent.service.LoginService.logout
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

/**
 * Service responsible for session authentication.
 *
 * The server is stateless aside from the `token` httpOnly cookie that carries
 * the upstream Egis bearer token between requests. This service establishes
 * that cookie on [login], removes it on [logout], and reports its presence via
 * [isLoggedIn]. Credentials are validated by the upstream SSP (Self-Service
 * Portal) API through [Api]; all operations emit metrics via [Metrics].
 */
object LoginService {

    private val LOG = LoggerFactory.getLogger(LoginService::class.java)

    /**
     * Enumeration of service methods exposed by [LoginService].
     *
     * Used by [Metrics] to tag log entries and counters with a consistent
     * service/method pair.
     */
    enum class Method : ServiceMethod {
        /** Report whether the request already carries a `token` cookie. */
        LoggedIn,
        /** Exchange credentials for a session token. */
        Login,
        /** Clear the session token. */
        Logout,
        ;
        override fun service(): String {
            return "Login"
        }
        override fun method(): String {
            return name
        }
    }

    /**
     * Reports whether the current request carries an authenticated session.
     *
     * Responds with a [Response] whose `success` flag reflects the presence of
     * the `token` cookie. This is a non-failing check: a missing token yields a
     * normal `200` response with `success = false`, not an error.
     *
     * @param call the incoming Ktor application call, inspected for the `token`
     *             cookie and used for metrics tagging.
     */
    suspend fun isLoggedIn(call: ApplicationCall) {
        if (call.request.cookies["token"] == null) {
            Metrics.logAndCount(call, Method.LoggedIn, Level.INFO, "NO_TOKEN")
            call.respond(Response(false, "Je bent niet ingelogd"))
            return
        }
        try {
            Api.get(call, "/v1/permit_overview/product_list") {
                parameter("page", "1")
                parameter("row_per_page", "1")
            }
        } catch (_: Exception) {
            call.clearTokenCookie()
            Metrics.logAndCount(call, Method.LoggedIn, Level.INFO, "TOKEN_EXPIRED")
            call.respond(Response(false, "Je hebt een verlopen token"))
            return
        }
        Metrics.logAndCount(call, Method.LoggedIn, Level.INFO, "TOKEN_FOUND")
        call.respond(Response(true, "Je hebt een valide token"))
        return
    }

    /**
     * Authenticates the user and establishes a session.
     *
     * The request body is parsed as a [LoginRequest] and forwarded to the
     * upstream `login_check` endpoint. On success the returned bearer token is
     * stored in an httpOnly `token` cookie and a successful [Response] is
     * returned. Invalid credentials yield a `401` and any other upstream
     * outcome yields a `500`, each with a localized message. If a `token`
     * cookie is already present it is logged as a warning but the login still
     * proceeds.
     *
     * @param call the incoming Ktor application call carrying the
     *             [LoginRequest] body; the response token cookie is written to
     *             it.
     */
    suspend fun login(call: ApplicationCall) {
        if (call.request.cookies["token"] != null) {
            Metrics.logAndCount(call, Method.Login, Level.WARN, "TOKEN_ALREADY_EXISTS")
        }
        val request = call.receive<LoginRequest>()
        val response = Api.post(call, "/ssp/login_check") {
            setBody(request)
        }
        when (response.status.value) {
            in 200..299 -> {
                val loginResponse: LoginResponse = response.body()
                call.response.cookies.append(
                    Cookie(
                        name = "token",
                        value = loginResponse.token,
                        encoding = CookieEncoding.DQUOTES,
                        httpOnly = true,
                        secure = false,
                    )
                )
                Metrics.logAndCount(call, Method.Login, Level.INFO, "SUCCESS")
                call.respond(Response(true, "Success"))
            }
            401 -> {
                Metrics.logAndCount(call, Method.Login, Level.WARN, "CREDENTIALS_INVALID")
                call.respond(
                    status = HttpStatusCode.Unauthorized,
                    message = Response(false, "Gebruikersnaam en/of wachtwoord onjuist")
                )
            }
            else -> {
                LOG.error("Unknown error: [${response.status}] ${response.body<String>()}")
                Metrics.logAndCount(call, Method.Login, Level.ERROR, "UNKNOWN_ERROR")
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = Response(false, "Unknown error")
                )
            }
        }
    }

    /**
     * Ends the current session by clearing the `token` cookie.
     *
     * If no `token` cookie is present the call is treated as already logged out
     * and a [Response] with `success = false` is returned; otherwise the cookie
     * is cleared and a successful [Response] is returned.
     *
     * @param call the incoming Ktor application call; the `token` cookie is
     *             cleared on its response.
     */
    suspend fun logout(call: ApplicationCall) {
        if (call.request.cookies["token"] == null) {
            Metrics.logAndCount(call, Method.Logout, Level.WARN, "NO_TOKEN")
            call.respond(Response(false, "Je bent niet ingelogd"))
            return
        }
        call.clearTokenCookie()
        Metrics.logAndCount(call, Method.Logout, Level.INFO, "LOGGED_OUT")
        call.respond(Response(true, "Je bent uitgelogd"))
        return
    }

}