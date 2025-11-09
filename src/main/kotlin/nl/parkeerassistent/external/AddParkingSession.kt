package nl.parkeerassistent.external

import kotlinx.serialization.Serializable

@Serializable
data class AddParkingSession(
    val reportCode: Long,
    val paymentZoneId: String,
    val vehicleId: String,
    val startDateTime: String,
    val endDateTime: String
)