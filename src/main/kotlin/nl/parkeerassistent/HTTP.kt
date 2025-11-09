package nl.parkeerassistent

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
        anyMethod()
        allowHeaders { true }
        allowOrigins { true }
        anyHost()
        allowCredentials = true
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
