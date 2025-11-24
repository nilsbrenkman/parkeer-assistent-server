package nl.parkeerassistent.service

import io.ktor.client.call.body
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
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

object LoginService {

    private val LOG = LoggerFactory.getLogger(LoginService::class.java)

    enum class Method : ServiceMethod {
        LoggedIn,
        Login,
        Logout,
        ;
        override fun service(): String {
            return "Login"
        }
        override fun method(): String {
            return name
        }
    }

    suspend fun isLoggedIn(call: ApplicationCall) {
        if (call.request.cookies["token"] == null) {
            Metrics.logAndCount(call, Method.LoggedIn, Level.INFO, "NO_TOKEN")
            call.respond(Response(false, "Je bent niet ingelogd"))
            return
        }
        Metrics.logAndCount(call, Method.LoggedIn, Level.INFO, "TOKEN_FOUND")
        call.respond(Response(true, "Je hebt een token"))
        return
    }

    suspend fun login(call: ApplicationCall) {
        if (call.request.cookies["token"] != null) {
            Metrics.logAndCount(call, Method.Login, Level.WARN, "TOKEN_ALREADY_EXISTS")
            call.respond(Response(false, "Je bent al ingelogd"))
            return
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