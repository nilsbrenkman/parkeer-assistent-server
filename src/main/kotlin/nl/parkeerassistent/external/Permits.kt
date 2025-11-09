package nl.parkeerassistent.external

import kotlinx.serialization.Serializable

@Serializable
data class Permits(
    val permits: List<Permit>,
    val wallet: Wallet = Wallet(0.0, "EUR")
)

