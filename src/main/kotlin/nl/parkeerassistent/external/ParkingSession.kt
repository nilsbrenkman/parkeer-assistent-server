package nl.parkeerassistent.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * "parking_session_id": 40900900,
 * "client_id": 2171334,
 * "client_product_id": 90148561,
 * "visitor_parking_session": false,
 * "can_edit": true,
 * "permit_name": "Bezoekersparkeervergunning",
 * "zone_description": "WM55 Oost-5a",
 * "started_at": "2025-11-17T09:00:00+00:00",
 * "ended_at": "2025-11-17T10:00:00+00:00",
 * "vrn": "R957KF",
 * "cost": 181,
 * "duration": 3600,
 * "user_id": null,
 * "ps_right_id": 7640614861,
 * "status": "FUTURE",
 * "created_at": "2025-11-15T02:09:55+00:00",
 * "updated_at": "2025-11-15T02:09:56+00:00",
 * "machine_number": 15505
 */
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