package nl.parkeerassistent.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CostEstimateResponse(
    @SerialName("parking_session_balance") val parkingSessionBalance: ParkingSessionBalance,
)