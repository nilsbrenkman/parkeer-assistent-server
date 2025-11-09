package nl.parkeerassistent.model

import kotlinx.serialization.Serializable

@Serializable
data class Parking(
    val id: Long,
    val license: String,
    val name: String? = null,
    val startTime: String,
    val endTime: String,
    val cost: Double
)