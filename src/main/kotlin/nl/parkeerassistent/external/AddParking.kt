package nl.parkeerassistent.external

import kotlinx.serialization.Serializable

/**
 * {
 *    "parkingsession":{
 *       "reportCode":804661,
 *       "paymentZoneId":"T14B",
 *       "vehicleId":"K213NF",
 *       "startDateTime":"2024-11-18T08:20:00.000Z",
 *       "endDateTime":"2024-11-18T18:00:00.000Z"
 *    },
 *    "locale":"nl"
 * }
 */
@Serializable
data class AddParking(
    val parkingsession: AddParkingSession,
    val locale: String,
)

