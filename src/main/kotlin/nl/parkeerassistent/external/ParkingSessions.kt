package nl.parkeerassistent.external

import kotlinx.serialization.Serializable

@Serializable
data class ParkingSessions(
    val parkingSession: List<ParkingSession> = emptyList()
)

