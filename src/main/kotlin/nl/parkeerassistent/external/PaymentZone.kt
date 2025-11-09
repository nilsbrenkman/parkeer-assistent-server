package nl.parkeerassistent.external

import kotlinx.serialization.Serializable

@Serializable
data class PaymentZone(
    val id: String,
    val description: String,
    val days: List<Day>
)