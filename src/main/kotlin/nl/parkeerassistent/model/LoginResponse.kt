package nl.parkeerassistent.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val token: String,
)