package nl.parkeerassistent.service

import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.MultiPart.FormData
import io.ktor.http.Parameters
import io.ktor.http.contentType
import nl.parkeerassistent.Metrics
import nl.parkeerassistent.client.Permit
import nl.parkeerassistent.client.Session
import nl.parkeerassistent.client.User
import nl.parkeerassistent.external.Credentials
import nl.parkeerassistent.external.Csrf
import nl.parkeerassistent.model.LoginRequest
import nl.parkeerassistent.model.Response
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

object LoginService {

    private val log = LoggerFactory.getLogger(LoginService::class.java)

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

    suspend fun isLoggedIn(session: Session): Response {

        if (session.hasValidToken()) {
            return Response(true, "Ingelogd")
        }

        val response = session.client.get(Session.getMainUrl("api/auth/session"))
        val result = response.body<nl.parkeerassistent.external.Session>()

        result.user?.let {
            if (it.scope != "permitHolder") {
                session.cookieStore.clear()
                Metrics.logAndCount(session.call, Method.LoggedIn, Level.INFO, "NOT_SUPPORTED")
                return Response(false, "Bezoekers account wordt niet ondersteund")
            }
            log.debug("token: ${it.access_token}")
            session.user = User(it.access_token)
            log.debug("reportcode: ${it.reportcode}")
            if (session.permit == null) {
                session.permit = Permit(it.reportcode, null, it.scope)
            }
            Metrics.logAndCount(session.call, Method.LoggedIn, Level.INFO, "LOGGED_IN")
            return Response(true, "Ingelogd")
        }

        Metrics.logAndCount(session.call, Method.LoggedIn, Level.INFO, "NOT_LOGGED_IN")
        return Response(false, "Niet ingelogd")
    }

    suspend fun login(session: Session, request: LoginRequest): Response {

        session.client.get(Session.getMainUrl("api/auth/session")) {}
        val csrfToken = getCsrfToken(session)

        val loggedIn = try {
            session.client.post(Session.getMainUrl("api/auth/callback/credentials")) {
                contentType(FormData)
                setBody(FormDataContent(formData = Parameters.build {
                    append("reportCode", request.username)
                    append("pin", request.password)
                    append("csrfToken", csrfToken)
                    append("json", "true")
                    append("callbackUrl", "https://aanmeldenparkeren.amsterdam.nl/login")
                }))
            }
            isLoggedIn(session)
        } catch (_: Exception) {
            Response(false, "Inloggen mislukt")
        }

        Metrics.logAndCount(session.call, Method.Login, Level.INFO, if (loggedIn.success) "LOGIN_SUCCESS" else "LOGIN_FAILED")
        return loggedIn
    }

    suspend fun logout(session: Session): Response {
        val csrfToken = getCsrfToken(session)

        val response = session.client.post(Session.getMainUrl("api/auth/signout")) {
            contentType(FormData)
            setBody(FormDataContent(formData = Parameters.build {
                append("csrfToken", csrfToken)
                append("json", "true")
                append("callbackUrl", "https://aanmeldenparkeren.amsterdam.nl/login")
            }))
        }

        val result: Credentials = response.body()
        if (result.url != "https://aanmeldenparkeren.amsterdam.nl/") {
            Metrics.logAndCount(session.call, Method.Login, Level.WARN, "LOGOUT_FAILED")
        }

        session.cookieStore.clear()
        session.user = null
        session.permit = null

        Metrics.logAndCount(session.call, Method.Logout, Level.INFO, "LOGGED_OUT")
        return Response(true)
    }

    private suspend fun getCsrfToken(session: Session): String {
        val result: Csrf = session.client.get(Session.getMainUrl("api/auth/csrf")) {}.body()
        return result.csrfToken
    }

}