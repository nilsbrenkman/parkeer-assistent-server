package nl.parkeerassistent.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ParkingSessionBalance(
    @SerialName("calculated_cost") val calculatedCost: Long,
    @SerialName("duration") val duration: Long,
    @SerialName("calculated_time") val calculatedTime: Long,
)