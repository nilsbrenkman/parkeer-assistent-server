package nl.parkeerassistent.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.addDefaultResponseValidation
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.http.CookieEncoding
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.util.AttributeKey
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import nl.parkeerassistent.JSON
import nl.parkeerassistent.util.ServiceUtil
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.Base64

class Session(val call: ApplicationCall) : Closeable {

    val cookieStore: PassthroughCookieStorage = PassthroughCookieStorage(call.request.cookies["session"])

    var user: User? = call.request.cookies["customerid"]?.let { decodeCookie(it) }
        set(value) {
            if (field != value) {
                updateCookie("customerid", value)
                field = value
            }
        }

    var permit: Permit? = call.request.cookies["permitid"]?.let { decodeCookie(it) }
        set(value) {
            updateCookie("permitid", value)
            field = value
        }

    val client: HttpClient = HttpClient(Java) {
        followRedirects = false
        engine {
            dispatcher = Dispatchers.IO
        }
        install(ContentNegotiation) {
            json(JSON)
        }
        install(HttpCookies) {
            storage = cookieStore
        }
        install(ClientLoggerPlugin)
        addDefaultResponseValidation()
    }

    fun hasValidToken(): Boolean {
        user?.token?.let {
            val jwt = JWT(it)
            if (! jwt.isExpired()) {
                return true
            }
            LOG.info("token expired")
            return false
        }
        return false
    }

    suspend fun updateSessionCookie() {
        cookieStore.createSetCookie()?.let {
            if (it.isEmpty()) {
                ServiceUtil.clearSessionCookie(call)
            } else {
                call.response.cookies.append("session", it)
            }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(Session::class.java)

        fun getMainUrl(url: String):String {
            return "https://aanmeldenparkeren.amsterdam.nl/$url"
        }
    }

    private inline fun <reified T> decodeCookie(cookie: String): T? {
        try {
            val json = String(Base64.getDecoder().decode(cookie), StandardCharsets.UTF_8)
            return Json.decodeFromString(json)
        } catch (_: SerializationException) {
            LOG.debug("Unable to decode cookie: ${cookie}")
            return null
        }
    }

    private inline fun <reified T> updateCookie(key: String, cookie: T?) {
        cookie?.let {
            val json = Json.encodeToString(it)
            val value = String(
                Base64.getUrlEncoder().encode(ByteBuffer.wrap(json.toByteArray(StandardCharsets.UTF_8))).array(),
                StandardCharsets.UTF_8
            )
            call.response.cookies.append(key, value)
        } ?: call.response.cookies.append(
            key,
            "",
            CookieEncoding.URI_ENCODING,
            0,
            GMTDate()
        )
    }

    override fun close() {
        client.close()
    }

}

val SESSION_ATTRIBUTE_KEY = AttributeKey<Session>("session")

fun ApplicationCall.session(): Session = attributes.computeIfAbsent(SESSION_ATTRIBUTE_KEY) { Session(this) }

suspend fun ApplicationCall.respondWithSession(response: Any) {
    session().updateSessionCookie()
    respond(response)
}
