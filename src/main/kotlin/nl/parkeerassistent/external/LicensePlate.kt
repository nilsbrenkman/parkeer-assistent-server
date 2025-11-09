package nl.parkeerassistent.external

import kotlinx.serialization.Serializable

@Serializable
data class LicensePlate(
    val vehicleId: String,
    val visitorName: String? = null,
    val reportCode: Long? = null
)