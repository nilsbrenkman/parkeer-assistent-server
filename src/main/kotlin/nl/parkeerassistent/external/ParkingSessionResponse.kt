package nl.parkeerassistent.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ParkingSessionResponse(
    @SerialName("parking_session_id") val id: Long,
)