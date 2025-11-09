package nl.parkeerassistent

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.LoggerFactory

fun main() {
    val log = LoggerFactory.getLogger("Application")

    val host = System.getenv("HOST") ?: "0.0.0.0"
    val port = System.getenv("PORT").toInt()
    log.info("Starting server: $host:$port")

    embeddedServer(Netty, host = host, port = port, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureHTTP()
    configureErrorHandling()
    configureMetrics()
    configureRouting()
}