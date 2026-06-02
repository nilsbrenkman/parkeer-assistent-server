package nl.parkeerassistent.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ParkingSessionRequest(
    val vrn: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("ended_at") val endedAt: String,
    @SerialName("client_product_id") val clientProductId: Long,
    @SerialName("zone_id") val zoneId: Long,
    @SerialName("machine_number") val machineNumber: Long,
    @SerialName("is_new_favorite_machine_number") val isNewFavorite: Boolean
)

