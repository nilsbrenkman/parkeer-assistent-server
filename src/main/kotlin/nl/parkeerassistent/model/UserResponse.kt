package nl.parkeerassistent.model

import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    val balance: String,
    val hourRate: Double,
    val productId: Long,
    val zoneId: Long,
    val parkingMeterId: Long,
    val regime: Regime,
)

