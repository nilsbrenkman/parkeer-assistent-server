package nl.parkeerassistent.model

import kotlinx.serialization.Serializable

@Serializable
data class PaymentRequest(
    val amount: String,
    val issuerId: String
)