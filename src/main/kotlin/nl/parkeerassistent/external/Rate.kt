package nl.parkeerassistent.external

import kotlinx.serialization.Serializable

@Serializable
data class Rate(
    val currency: String,
    val value: Double
)