package nl.parkeerassistent.util

import kotlinx.serialization.json.Json
import org.slf4j.Logger

inline fun <reified T> Logger.json(key: String, obj: T) {
    if (isDebugEnabled) {
        debug("${key}: ${Json.encodeToString(obj)}")
    }
}
