package nl.parkeerassistent.model

import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    val balance: String,
    val hourRate: Double,
    val regimeTimeStart: String,
    val regimeTimeEnd: String,
    val regime: Regime
)

