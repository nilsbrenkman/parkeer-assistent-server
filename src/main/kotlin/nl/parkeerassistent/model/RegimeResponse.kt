package nl.parkeerassistent.model

import kotlinx.serialization.Serializable

@Serializable
data class RegimeResponse (
    val hourRate: Double,
    val zoneId: Long,
    val regime: Regime,
)