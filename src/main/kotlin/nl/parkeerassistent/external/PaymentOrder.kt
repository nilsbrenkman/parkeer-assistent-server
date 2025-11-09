package nl.parkeerassistent.external

import kotlinx.serialization.Serializable

@Serializable
data class PaymentOrder(
    val frontendId: Long,
    val redirectUrl: String,
    val orderStatus: String,
    val orderType: String
)