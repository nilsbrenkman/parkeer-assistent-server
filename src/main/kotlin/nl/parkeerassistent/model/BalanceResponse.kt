package nl.parkeerassistent.model

import kotlinx.serialization.Serializable

@Serializable
data class BalanceResponse(val balance: String)