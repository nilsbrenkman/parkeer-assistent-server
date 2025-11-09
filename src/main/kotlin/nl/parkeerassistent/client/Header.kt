package nl.parkeerassistent.client

import kotlinx.serialization.Serializable

@Serializable
data class Header(
    val typ: String,
    val alg: String
)