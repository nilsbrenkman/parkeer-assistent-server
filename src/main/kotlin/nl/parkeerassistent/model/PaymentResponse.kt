package nl.parkeerassistent.model

import kotlinx.serialization.Serializable

@Serializable
data class PaymentResponse(
    val redirectUrl: String,
    val transactionId: String
)