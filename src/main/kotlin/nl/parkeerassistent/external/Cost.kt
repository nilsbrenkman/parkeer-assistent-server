package nl.parkeerassistent.external

import kotlinx.serialization.Serializable

@Serializable
data class Cost(
    val currency: String,
    val value: Double
)