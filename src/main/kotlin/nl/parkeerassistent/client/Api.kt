package nl.parkeerassistent.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.addDefaultResponseValidation
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.CookieEncoding
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationCall
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.Dispatchers
import nl.parkeerassistent.JSON
import org.slf4j.LoggerFactory

object Api {

    private val LOG = LoggerFactory.getLogger("Api")

    suspend fun get(call: ApplicationCall, url: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
        return client.get(getUrl(url)) {
            addHeaders(this, call)
            apply(block)
        }
    }

    suspend fun post(call: ApplicationCall, url: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
        return client.post(getUrl(url)) {
            addHeaders(this, call)
            apply(block)
        }
    }

    suspend fun patch(call: ApplicationCall, url: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
        return client.patch(getUrl(url)) {
            addHeaders(this, call)
            apply(block)
        }
    }

    suspend fun delete(call: ApplicationCall, url: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
        return client.delete(getUrl(url)) {
            addHeaders(this, call)
            apply(block)
        }
    }


    private const val BASE_URL = "https://api.parkeervergunningen.egisparkingservices.nl/api"

    private val client = HttpClient(Java) {
        followRedirects = false
        engine {
            dispatcher = Dispatchers.IO
        }
        install(ContentNegotiation) {
            json(JSON)
        }
        install(ClientLoggerPlugin)
        addDefaultResponseValidation()
    }

    private fun getUrl(url: String):String {
        return "$BASE_URL$url"
    }

    private fun addHeaders(httpRequestBuilder: HttpRequestBuilder, call: ApplicationCall) {
        call.request.cookies["token"]?.let {
            httpRequestBuilder.header("Authorization", "Bearer ${it}")
        }
        httpRequestBuilder.contentType(ContentType.Application.Json)
    }

}

fun ApplicationCall.clearTokenCookie() {
    response.cookies.append(
        "token",
        "",
        CookieEncoding.DQUOTES,
        0,
        GMTDate()
    )
}
