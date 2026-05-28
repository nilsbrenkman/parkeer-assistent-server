package nl.parkeerassistent

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import nl.parkeerassistent.service.ServiceMethod
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

object Metrics {

    private val LOG = LoggerFactory.getLogger("MetricsEvent")

    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    fun logAndCount(call: ApplicationCall, serviceMethod: ServiceMethod, level: Level = Level.INFO, message: String) {
        LOG.info(
            mapOf(
                Pair("service", serviceMethod.service()),
                Pair("method", serviceMethod.method()),
                Pair("level", level.name),
                Pair("message", message),
            ).toString()
        )

        appMicrometerRegistry.counter(
            "parkeerassistent_api_requests", Tags.of(
                Tag.of("service", serviceMethod.service()),
                Tag.of("method", serviceMethod.method()),
                Tag.of("level", level.name),
                Tag.of("message", message),
            )
        ).increment()

        val os = call.request.headers["X-ParkeerAssistent-OS"] ?: "null"
        val sdk = call.request.headers["X-ParkeerAssistent-SDK"] ?: "null"
        val version = call.request.headers["X-ParkeerAssistent-Version"] ?: "null"
        val build = call.request.headers["X-ParkeerAssistent-Build"] ?: "0"

        appMicrometerRegistry.counter(
            "parkeerassistent_device_data", Tags.of(
                Tag.of("os", os),
                Tag.of("sdk", sdk),
                Tag.of("version", version),
                Tag.of("build", build),
            )
        ).increment()

    }
}

fun Application.configureMetrics() {
    install(MicrometerMetrics) {
        registry = Metrics.appMicrometerRegistry
    }
    routing {
        get("/metrics") {
            call.respond(Metrics.appMicrometerRegistry.scrape())
        }
    }
}
