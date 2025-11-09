package nl.parkeerassistent.service

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondText

object VersionService {

    private val currentVersion = parse(System.getenv("VERSION"))

    suspend fun version(call: ApplicationCall) {
        val version = parse(call.parameters["version"]!!)

        if (isEnabled(version)) {
            call.respondText("live", status = HttpStatusCode.OK)
        } else {
            call.respondText("mock", status = HttpStatusCode.NotFound)
        }
    }

    private fun parse(version: String): Version {
        val parts = version.split(".")
        if (parts.size != 3) {
            throw IllegalArgumentException("Invalid version")
        }
        return Version(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
    }

	private fun isEnabled(version: Version): Boolean {
        if (version.major == currentVersion.major) {
            if (version.minor == currentVersion.minor) {
                return version.patch <= currentVersion.patch
            }
            return version.minor < currentVersion.minor
        }
		return version.major < currentVersion.major
	}
}

private data class Version(val major: Int, val minor: Int, val patch: Int)