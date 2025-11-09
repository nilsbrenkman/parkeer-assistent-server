package nl.parkeerassistent.external

import kotlinx.serialization.Serializable

@Serializable
data class Balance(
    val amount: Double,
    val currency: String
)