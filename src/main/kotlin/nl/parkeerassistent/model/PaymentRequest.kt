package nl.parkeerassistent.model

import kotlinx.serialization.Serializable

@Serializable
data class PaymentRequest(
    val amount: Long,
    val brand: String,
    val lang: String,
)