package nl.parkeerassistent.external

import kotlinx.serialization.Serializable

@Serializable
data class Csrf(val csrfToken: String)
