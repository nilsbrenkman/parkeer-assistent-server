package nl.parkeerassistent

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.plugins.origin
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import nl.parkeerassistent.client.clearTokenCookie
import org.slf4j.LoggerFactory

private val LOG = LoggerFactory.getLogger("HTTP")

fun Application.configureErrorHandling() {

    intercept(ApplicationCallPipeline.Monitoring) {
        try {
            LOG.info("Request: ${call.request.httpMethod.value} ${call.request.uri} from ${call.request.origin.remoteHost}")
            proceed()
        } catch (e: RedirectResponseException) {
            LOG.warn("Not logged in [${e.message}]")
            call.clearTokenCookie()
            call.respond(HttpStatusCode.Unauthorized, "Not authorized")
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Unauthorized) {
                LOG.warn("Not logged in")
                call.clearTokenCookie()
                call.respond(HttpStatusCode.Unauthorized, "Not authorized")
            } else {
                LOG.warn("Client error", e)
                call.respond(e.response.status, "Client error")
            }
        } catch (e: ServerResponseException) {
            LOG.warn("Server error", e)
            call.respond(e.response.status, "Server error")
        } catch (e: Throwable) {
            LOG.error("Unhandled exception", e)
            call.respond(HttpStatusCode.InternalServerError, "Internal Server Error")
        }
    }

}