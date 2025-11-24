package nl.parkeerassistent.external

import kotlinx.serialization.Serializable

@Serializable
data class ParkingZoneResponse(
    val zone: Zone,
)