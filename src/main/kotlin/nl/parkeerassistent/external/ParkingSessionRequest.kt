package nl.parkeerassistent.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * {"vrn":"R957KF","ended_at":"Mon, 17 Nov 2025 10:00:00 GMT","started_at":"Mon, 17 Nov 2025 09:00:00 GMT","client_product_id":90148561,"zone_id":55,"machine_number":15505,"is_new_favorite_machine_number":true}
 */
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

