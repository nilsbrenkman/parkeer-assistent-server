package nl.parkeerassistent.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val type: String,
    val id: Long,
    val zone: String,
    @SerialName("permit_description") val permitDescription: String,
    @SerialName("permit_name") val permitName: String,
    @SerialName("permit_type") val permitType: String,
    val status: String,
)
