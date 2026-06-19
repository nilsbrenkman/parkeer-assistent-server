package nl.parkeerassistent.model

import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    val balance: String,
    val productId: Long,
    val parkingMeterId: Long?,
    val zoneId: Long?,
    val hourRate: Double?,
    val regime: Regime?,
)

