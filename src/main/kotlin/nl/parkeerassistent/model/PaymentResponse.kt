package nl.parkeerassistent.model

import kotlinx.serialization.Serializable

@Serializable
data class PaymentResponse(
    val url: String,
)