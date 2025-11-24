package nl.parkeerassistent.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CostEstimateRequest(
    @SerialName("client_product_id") val productId: Long,
    @SerialName("started_at") val startedAt: String,
    @SerialName("ended_at") val endedAt: String,
    @SerialName("machine_number") val parkingMeterId: Long,
)