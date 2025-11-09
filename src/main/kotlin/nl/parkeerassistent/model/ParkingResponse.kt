package nl.parkeerassistent.model

import kotlinx.serialization.Serializable

@Serializable
data class ParkingResponse(
    val active: List<Parking>,
    val scheduled: List<Parking>
)