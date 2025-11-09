package nl.parkeerassistent.external

import kotlinx.serialization.Serializable

@Serializable
data class User (
    val access_token:String,
    val reportcode: Long,
    val family_name: String,
    val initials: String? = null,
    val scope: String
)