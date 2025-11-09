package nl.parkeerassistent.external

import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val user: User? = null,
    val expires: String? = null,
    val accessToken: String? = null,
    val error: String? = null
)