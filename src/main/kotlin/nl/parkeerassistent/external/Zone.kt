package nl.parkeerassistent.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Zone(
    @SerialName("zone_id") val zoneId: Long,
    @SerialName("zone_description") val description: String,
    @SerialName("time_frame_data") val timeFrameData: List<List<TimeFrameData>>,
)