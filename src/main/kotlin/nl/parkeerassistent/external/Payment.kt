package nl.parkeerassistent.external

import kotlinx.serialization.Serializable

@Serializable
data class Payment(
    val balance: Balance,
    val redirect: Redirect
)

