package nl.parkeerassistent.model

import kotlinx.serialization.Serializable

@Serializable
data class Regime(
    val days: List<RegimeDay>
)