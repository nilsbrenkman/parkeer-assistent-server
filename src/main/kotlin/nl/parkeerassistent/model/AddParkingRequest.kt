package nl.parkeerassistent.model

import kotlinx.serialization.Serializable

@Serializable
data class AddParkingRequest(
    val visitor: Visitor,
    val timeMinutes: Int,
    val start: String? = null,
    val regimeTimeEnd: String
) {}