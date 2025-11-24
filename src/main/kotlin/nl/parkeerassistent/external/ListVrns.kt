package nl.parkeerassistent.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ListVrns(
    val count: Int,
    @SerialName("favorite_vrns") val vrns: List<Vrn>
)

