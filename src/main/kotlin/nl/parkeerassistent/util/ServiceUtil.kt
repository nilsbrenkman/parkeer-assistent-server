package nl.parkeerassistent.util

import io.ktor.http.CookieEncoding
import io.ktor.server.application.ApplicationCall
import io.ktor.util.date.GMTDate
import nl.parkeerassistent.Metrics
import nl.parkeerassistent.model.Response
import nl.parkeerassistent.service.ServiceMethod
import org.slf4j.event.Level

object ServiceUtil {

    fun convertResponse(call: ApplicationCall, serviceMethod: ServiceMethod, response: Boolean): Response {
        if (response) {
            Metrics.logAndCount(call, serviceMethod, Level.INFO,"SUCCESS")
        } else {
            Metrics.logAndCount(call, serviceMethod, Level.WARN, "FAILED")
        }
        return Response(response)
    }

    fun clearSessionCookie(call: ApplicationCall) {
        call.response.cookies.append(
            "session",
            "",
            CookieEncoding.URI_ENCODING,
            0,
            GMTDate()
        )
    }

}