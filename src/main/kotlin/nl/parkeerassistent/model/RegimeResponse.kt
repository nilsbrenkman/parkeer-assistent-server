package nl.parkeerassistent.model

import kotlinx.serialization.Serializable

@Serializable
data class RegimeResponse (
    val zoneId: Long?,
    val hourRate: Double?,
    val regime: Regime?,
)