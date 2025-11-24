package nl.parkeerassistent.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StopParkingRequest(
    @SerialName("new_ended_at") val newEndedAt: String
)

