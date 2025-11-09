package nl.parkeerassistent.client

import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64

class JWT(token: String) {

    val header: Header
    val payload: Payload

    init {
        val split = token.split(".")
        header = decode(split[0])
        payload = decode(split[1])
    }

    private inline fun <reified T> decode(part: String): T {
        val json = String(Base64.getDecoder().decode(part), StandardCharsets.UTF_8)
        return Json.decodeFromString(json)
    }

    fun isExpired(): Boolean {
        val expiry = Instant.ofEpochSecond(payload.exp)
        return expiry.isBefore(Instant.now())
    }

}

