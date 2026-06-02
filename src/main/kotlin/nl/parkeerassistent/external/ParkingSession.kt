package nl.parkeerassistent.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ParkingSession(
    @SerialName("parking_session_id") val id: Long,
    @SerialName("client_id") val clientId: Long,
    @SerialName("client_product_id") val productId: Long,
    @SerialName("started_at") val startDate: String,
    @SerialName("ended_at") val endDate: String,
    val vrn: String,
    val cost: Long,
    val duration: Long,
    val status: String,
    @SerialName("machine_number") val machineNumber: Long? = null,
)