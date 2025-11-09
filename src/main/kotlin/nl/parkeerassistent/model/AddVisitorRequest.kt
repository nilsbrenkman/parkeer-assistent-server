package nl.parkeerassistent.model

import kotlinx.serialization.Serializable

@Serializable
data class AddVisitorRequest(
    val license: String,
    val name: String
) {
}