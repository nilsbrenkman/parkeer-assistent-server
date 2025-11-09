package nl.parkeerassistent.client

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.api.createClientPlugin
import org.slf4j.LoggerFactory

private val LOGGER = LoggerFactory.getLogger("HttpClient")

val ClientLoggerPlugin = createClientPlugin("CombinedLoggingPlugin") {
    onResponse { httpResponse ->
        LOGGER.info("Request: ${httpResponse.call.request.method.value} ${httpResponse.call.request.url} "
                + "=> ${httpResponse.status} in ${httpResponse.responseTime.timestamp - httpResponse.requestTime.timestamp} ms")
        if (httpResponse.status.value == 401) {
            throw ClientRequestException(httpResponse, "Not authorized")
        }
    }
}