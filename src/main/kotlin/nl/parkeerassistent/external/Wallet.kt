package nl.parkeerassistent.external

import kotlinx.serialization.Serializable

@Serializable
data class Wallet(
    val balance: Double,
    val currency: String
)