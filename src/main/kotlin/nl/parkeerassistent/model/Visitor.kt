package nl.parkeerassistent.model

import kotlinx.serialization.Serializable

@Serializable
data class Visitor(
    val id: Long,
    val license: String,
    val formattedLicense: String,
    val name: String? = null
)