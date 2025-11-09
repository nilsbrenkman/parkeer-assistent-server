package nl.parkeerassistent.model

import kotlinx.serialization.Serializable

@Serializable
data class CompleteRequest(
    val transactionId: String,
    val data: String
)