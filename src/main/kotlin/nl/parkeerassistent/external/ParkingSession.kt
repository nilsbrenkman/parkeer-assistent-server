package nl.parkeerassistent.external

import kotlinx.serialization.Serializable

@Serializable
data class ParkingSession(
    val psRightId: Long,
    val paymentZoneId: String? = null,
    val startDate: String,
    val endDate: String,
    val vehicleId: String,
    val visitorName: String? = null,
    val parkingCost: Cost,
    val isCancelled: Boolean
)