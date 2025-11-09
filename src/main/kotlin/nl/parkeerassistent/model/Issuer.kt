package nl.parkeerassistent.model

import kotlinx.serialization.Serializable

@Serializable
data class Issuer(
    val issuerId: String,
    val name: String
)