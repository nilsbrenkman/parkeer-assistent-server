package nl.parkeerassistent.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.addDefaultResponseValidation
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.MissingRequestParameterException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.parkeerassistent.JSON
import nl.parkeerassistent.external.Order
import org.slf4j.LoggerFactory

object CloudApi {

    private val LOG = LoggerFactory.getLogger("CloudApi")

    suspend fun get(session: Session, url: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
        return client.get(getCloudUrl(url)) {
            addCloudHeaders(this, session)
            apply(block)
        }
    }

    suspend fun post(session: Session, url: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
        return client.post(getCloudUrl(url)) {
            addCloudHeaders(this, session)
            apply(block)
        }
    }

    suspend fun delete(session: Session, url: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse {
        return client.delete(getCloudUrl(url)) {
            addCloudHeaders(this, session)
            apply(block)
        }
    }

    private val cloudBaseUrl = "https://evs-ssp.mendixcloud.com/rest/sspapi/"

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

    private fun getCloudUrl(url: String):String {
        return cloudBaseUrl + url
    }

    private fun addCloudHeaders(httpRequestBuilder: HttpRequestBuilder, session: Session) {
        val token = ensureData(session.user?.token, "token")
        httpRequestBuilder.header("Authorization", token)
        httpRequestBuilder.contentType(ContentType.Application.Json)
    }

    suspend fun waitForOrder(session: Session, orderId: Long): Boolean {
        repeat(5) {
            withContext(Dispatchers.IO) {
                Thread.sleep(200L * (it + 1))
            }
            val order = get(session, "v1/orders/$orderId").body<Order>()
            if (order.orderStatus == "Completed") {
                LOG.debug("Order confirmed in ${it + 1} tries")
                return true
            }
        }
        return false
    }

}

inline fun <reified T> ensureData(data: T?, name: String): T {
    return data ?: throw MissingRequestParameterException(name)
}