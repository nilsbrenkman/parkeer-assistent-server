package nl.parkeerassistent.model

import kotlinx.serialization.Serializable

@Serializable
data class RegimeDay(
    val weekday: String,
    val startTime: String,
    val endTime: String
)