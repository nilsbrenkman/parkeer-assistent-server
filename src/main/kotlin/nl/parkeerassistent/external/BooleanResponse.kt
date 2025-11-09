package nl.parkeerassistent.external

import kotlinx.serialization.Serializable

@Serializable
data class BooleanResponse(val successful: Boolean)