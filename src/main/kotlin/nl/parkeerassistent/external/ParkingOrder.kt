package nl.parkeerassistent.external

import kotlinx.serialization.Serializable

@Serializable
data class ParkingOrder(
    val frontendId: Long,
    val orderStatus: String,
    val orderType: String
)