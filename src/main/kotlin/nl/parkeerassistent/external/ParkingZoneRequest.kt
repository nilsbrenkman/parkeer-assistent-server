package nl.parkeerassistent.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ParkingZoneRequest(
    @SerialName("client_product_id") val productId: Long,
    @SerialName("machine_number") val parkingMeterId: Long,
)