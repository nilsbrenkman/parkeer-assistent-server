package nl.parkeerassistent.external

import kotlinx.serialization.Serializable

@Serializable
data class Day(
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String
)