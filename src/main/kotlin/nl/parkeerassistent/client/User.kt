package nl.parkeerassistent.client

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val token: String
)