package nl.parkeerassistent.model

import kotlinx.serialization.Serializable

@Serializable
data class VisitorResponse(val visitors: List<Visitor>)