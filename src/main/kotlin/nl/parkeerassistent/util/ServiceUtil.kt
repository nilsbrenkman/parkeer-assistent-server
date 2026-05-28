package nl.parkeerassistent.util

import io.ktor.http.CookieEncoding
import io.ktor.server.application.ApplicationCall
import io.ktor.util.date.GMTDate

object ServiceUtil {

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