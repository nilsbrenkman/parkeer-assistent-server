package nl.parkeerassistent.external

import kotlinx.serialization.Serializable

@Serializable
data class TimeFrameData(
    val startTime: Long? = null,
    val endTime: Long? = null,
)