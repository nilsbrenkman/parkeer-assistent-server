package nl.parkeerassistent.client

import kotlinx.serialization.Serializable

@Serializable
data class Permit(
    val reportCode: Long,
    val paymentZoneId: String? = null,
    val scope: String? = null
)