package nl.parkeerassistent.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val username: String,
    val pin: String,
    @SerialName("money_balance") val moneyBalance: Long? = null,
)