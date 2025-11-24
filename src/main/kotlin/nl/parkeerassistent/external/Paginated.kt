package nl.parkeerassistent.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Paginated<D>(
    val page: Int,
    @SerialName("row_per_page") val rowPerPage: Int,
    val count: Int,
    val data: List<D>,
)
