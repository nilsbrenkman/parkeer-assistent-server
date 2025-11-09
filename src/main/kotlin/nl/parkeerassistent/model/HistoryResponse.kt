package nl.parkeerassistent.model

import kotlinx.serialization.Serializable

@Serializable
data class HistoryResponse(
    val history: List<Parking>
)