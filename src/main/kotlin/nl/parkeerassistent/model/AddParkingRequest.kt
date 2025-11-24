package nl.parkeerassistent.model

import kotlinx.serialization.Serializable

@Serializable
data class AddParkingRequest(
    val license: String,
    val timeMinutes: Int,
    val start: String? = null,
    val productId: Long,
    val zoneId: Long,
    val parkingMeterId: Long,
)