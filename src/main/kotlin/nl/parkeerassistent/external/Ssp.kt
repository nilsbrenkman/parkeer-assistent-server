package nl.parkeerassistent.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Ssp(
    @SerialName("favorite_machine_number") val parkingMeterId: Long,
    @SerialName("main_account") val mainAccount: Account,
)