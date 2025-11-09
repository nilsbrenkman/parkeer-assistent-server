package nl.parkeerassistent.external

import kotlinx.serialization.Serializable

@Serializable
data class Permit(
    val reportCode: Long,
    val paymentZones: List<PaymentZone>,
    val parkingRate: Rate
)