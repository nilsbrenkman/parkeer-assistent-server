package nl.parkeerassistent

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.forwardedheaders.ForwardedHeaders
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.request.header
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

val JSON = Json {
    ignoreUnknownKeys = true
    allowSpecialFloatingPointValues = true
}

fun Application.configureHTTP() {
    install(Compression) {
        gzip()
    }
    install(ContentNegotiation) {
        json(JSON)
    }
    install(CORS) {
        allowOrigins { true }
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeaders { true }
        allowCredentials = true
        anyHost()
    }
    install(ForwardedHeaders)
    install(XForwardedHeaders)
    install(RateLimit) {
        register {
            rateLimiter(limit = 60, refillPeriod = 60.seconds)
            requestKey { call ->
                "${call.request.origin.remoteHost}|${call.request.header("User-Agent") ?: ""}"
            }
        }
    }

}
