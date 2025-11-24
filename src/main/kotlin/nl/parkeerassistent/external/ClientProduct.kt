package nl.parkeerassistent.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClientProduct(
    @SerialName("client_product_id") val clientProductId: Int,
    val ssp: Ssp,
)

