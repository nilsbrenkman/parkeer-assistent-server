package nl.parkeerassistent.service

import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.server.plugins.NotFoundException
import nl.parkeerassistent.Metrics
import nl.parkeerassistent.client.CloudApi
import nl.parkeerassistent.client.Session
import nl.parkeerassistent.client.ensureData
import nl.parkeerassistent.external.AddParking
import nl.parkeerassistent.external.AddParkingSession
import nl.parkeerassistent.external.ParkingOrder
import nl.parkeerassistent.external.ParkingSession
import nl.parkeerassistent.external.ParkingSessions
import nl.parkeerassistent.external.StopParking
import nl.parkeerassistent.external.StopParkingSession
import nl.parkeerassistent.model.AddParkingRequest
import nl.parkeerassistent.model.HistoryResponse
import nl.parkeerassistent.model.Parking
import nl.parkeerassistent.model.ParkingResponse
import nl.parkeerassistent.model.Response
import nl.parkeerassistent.util.DateUtil
import nl.parkeerassistent.util.ServiceUtil
import nl.parkeerassistent.util.json
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.time.Instant
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.Date

object ParkingService {

    private val log = LoggerFactory.getLogger(ParkingService::class.java)

    enum class Method: ServiceMethod {
        Get,
        Start,
        Stop,
        History,
        ;
        override fun service(): String {
            return "Parking"
        }
        override fun method(): String {
            return name
        }
    }

    suspend fun get(session: Session): ParkingResponse {

        val active = getParkingSessions(session, ParkingSessionType.Active)
        val scheduled = getParkingSessions(session, ParkingSessionType.Scheduled)

        Metrics.logAndCount(session.call, Method.Get, Level.INFO, "SUCCESS")
        return ParkingResponse(active, scheduled)
    }

    private suspend fun getParkingSessions(session: Session, type: ParkingSessionType): List<Parking> {

        val result = parkingSessions(session, type)

        val parkingSessions = if (type != ParkingSessionType.Completed) {
            result.sortedBy { ZonedDateTime.parse(it.startDate, DateUtil.gmtDateFormatter) }
        } else {
            result.sortedByDescending { ZonedDateTime.parse(it.startDate, DateUtil.gmtDateFormatter) }
        }

        return parkingSessions.map {
            Parking(it.psRightId, it.vehicleId, it.visitorName, convertTime(it.startDate), convertTime(it.endDate), it.parkingCost.value)
        }
    }

    enum class ParkingSessionType(val status: String) {
        Active("Actief"),
        Scheduled("Toekomstig"),
        Completed("Voltooid")
    }

    private fun convertTime(gmt: String): String {
        val date = DateUtil.gmtDateFormatter.parse(gmt)
        return DateUtil.dateFormatter.format(date)
    }

    suspend fun start(session: Session, request: AddParkingRequest): Response {
        val reportCode = ensureData(session.permit?.reportCode, "report code")
        val paymentZoneId = ensureData(session.permit?.paymentZoneId, "payment zone id")

        val calendar = Calendar.getInstance()
        calendar.time = request.start?.let { start -> DateUtil.dateTime.parse(start) } ?: run { Date() }
        calendar.add(Calendar.SECOND, 1)
        val start = calendar.time
        calendar.add(Calendar.MINUTE, request.timeMinutes)
        var end = calendar.time

        val regimeEnd = DateUtil.dateTime.parse(request.regimeTimeEnd)
        if (end.after(regimeEnd)) {
            end = regimeEnd
        }

        val parkingSession = AddParking(
            AddParkingSession(
                reportCode,
                paymentZoneId,
                request.visitor.license,
                DateUtil.gmtDateFormatter.format(start.toInstant()),
                DateUtil.gmtDateFormatter.format(end.toInstant())
            ),
            "nl"
        )

        log.json("parkingSession", parkingSession)

        val result: ParkingOrder = CloudApi.post(session, "v1/orders") {
            setBody(parkingSession)
        }.body()

        log.json("result", result)

        val completed = CloudApi.waitForOrder(session, result.frontendId)

        return ServiceUtil.convertResponse(session.call, Method.Start, completed)
    }

    suspend fun stop(session: Session): Response {
        val parkingId = ensureData(session.call.parameters["id"]?.toLong(), "parking id")
        val reportCode = ensureData(session.permit?.reportCode, "report code")

        val original = findParkingSession(session, parkingId) ?: throw NotFoundException("Session not found")

        val parkingSession = StopParking(
            StopParkingSession(
                reportCode,
                parkingId,
                original.startDate,
                DateUtil.gmtDateFormatter.format(Instant.now())
            )
        )

        val result: ParkingOrder = CloudApi.post(session, "v1/orders") {
            setBody(parkingSession)
        }.body()

        val completed = CloudApi.waitForOrder(session, result.frontendId)

        return ServiceUtil.convertResponse(session.call, Method.Stop, completed)
    }

    private suspend fun findParkingSession(session: Session, parkingId: Long): ParkingSession? {
        val parkingSession = parkingSessions(session, ParkingSessionType.Active)
            .find { p -> (p.psRightId == parkingId) }
        return parkingSession ?: parkingSessions(session, ParkingSessionType.Scheduled)
            .find { p -> (p.psRightId == parkingId) }
    }

    private suspend fun parkingSessions(session: Session, type: ParkingSessionType): List<ParkingSession> {
        val response = CloudApi.get(session, "v2/parkingsessions") {
            parameter("status", type.status)
            parameter("itemsPerPage", 100)
            parameter("page", 1)
        }
        val result: ParkingSessions = response.body()
        return result.parkingSession.filterNot { p -> p.isCancelled && p.parkingCost.value == 0.0 }
    }

    suspend fun history(session: Session): HistoryResponse {
        val history = getParkingSessions(session, ParkingSessionType.Completed)

        Metrics.logAndCount(session.call, Method.History, Level.INFO, "SUCCESS")
        return HistoryResponse(history)
    }

}
