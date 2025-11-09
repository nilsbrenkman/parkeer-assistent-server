package nl.parkeerassistent.external

import kotlinx.serialization.Serializable

@Serializable
data class Redirect(
    val merchantReturnUrl: String
)