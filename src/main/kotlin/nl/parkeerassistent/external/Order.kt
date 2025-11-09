package nl.parkeerassistent.external

import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val orderId: Long,
    val orderStatus: String,
    val orderType: String
)