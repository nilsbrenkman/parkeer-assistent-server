package nl.parkeerassistent.client

import kotlinx.serialization.Serializable

@Serializable
data class Payload(
    val sub: String,
    val iss: String,
    val exp: Long,
    val iat: Long
)