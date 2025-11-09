package nl.parkeerassistent.model

import kotlinx.serialization.Serializable

@Serializable
data class IdealResponse(
    val amounts: List<String>,
    val issuers: List<Issuer>
)