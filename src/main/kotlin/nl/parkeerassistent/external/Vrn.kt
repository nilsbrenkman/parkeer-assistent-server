package nl.parkeerassistent.external

import kotlinx.serialization.Serializable

@Serializable
data class Vrn(
    val id: Long? = null,
    val vrn: String,
    val description: String,
)