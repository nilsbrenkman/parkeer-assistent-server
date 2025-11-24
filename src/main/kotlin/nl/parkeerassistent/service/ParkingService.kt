package nl.parkeerassistent.service

import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import nl.parkeerassistent.Metrics
import nl.parkeerassistent.client.Api
import nl.parkeerassistent.external.Paginated
import nl.parkeerassistent.external.ParkingSession
import nl.parkeerassistent.external.ParkingSessionRequest
import nl.parkeerassistent.external.StopParkingRequest
import nl.parkeerassistent.model.AddParkingRequest
import nl.parkeerassistent.model.HistoryResponse
import nl.parkeerassistent.model.Parking
import nl.parkeerassistent.model.ParkingResponse
import nl.parkeerassistent.model.Response
import nl.parkeerassistent.util.DateUtil
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date

object ParkingService {

    private val LOG = LoggerFactory.getLogger(ParkingService::class.java)

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

    suspend fun get(call: ApplicationCall): ParkingResponse {
        val parkingSessions = retrieveParkingSessions(call)

        val active = parkingSessions.filter { it.status == "ACTIVE" }.map(this::convertParkingSession)
        val scheduled = parkingSessions.filter { it.status == "FUTURE" }.map(this::convertParkingSession)

        Metrics.logAndCount(call, Method.Get, Level.INFO, "SUCCESS")
        return ParkingResponse(active, scheduled)
    }

    private suspend fun retrieveParkingSessions(call: ApplicationCall): List<ParkingSession> {
        val productId = call.request.cookies["product_id"] ?: throw Exception("product_id is required")
        val response = Api.get(call, "/v1/ssp/parking_session/list") {
            parameter("page", "1")
            parameter("row_per_page", "100")
            parameter("sort", "parking_session_id:desc")
            parameter("filters[client_product_id]", productId)
        }
        val parkingSessions = response.body<Paginated<ParkingSession>>().data
        return parkingSessions
    }

    private fun convertParkingSession(parkingSession: ParkingSession): Parking = Parking(
        id = parkingSession.id,
        license = parkingSession.vrn,
        startTime = convertTime(parkingSession.startDate),
        endTime = convertTime(parkingSession.endDate),
        cost = parkingSession.cost / 100.0,
    )

    private fun convertTime(gmt: String): String {
        val date = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(gmt)
        return DateUtil.dateFormatter.format(date)
    }

    suspend fun start(call: ApplicationCall): Response {
        val request = call.receive<AddParkingRequest>()

        val calendar = Calendar.getInstance()
        calendar.time = request.start?.let { start -> DateUtil.dateTime.parse(start) } ?: Date()
        calendar.add(Calendar.SECOND, 1)
        val start = calendar.time
        calendar.add(Calendar.MINUTE, request.timeMinutes)
        val end = calendar.time

        val parkingSessionRequest = ParkingSessionRequest(
            vrn = request.license,
            startedAt = DateUtil.rfc1123Formatter.format(start.toInstant()),
            endedAt = DateUtil.rfc1123Formatter.format(end.toInstant()),
            clientProductId = request.productId,
            zoneId = request.zoneId,
            machineNumber = request.parkingMeterId,
            isNewFavorite = true,
        )
        Api.post(call, "/v1/ssp/parking_session/start") {
            setBody(parkingSessionRequest)
        }
        Metrics.logAndCount(call, Method.Start, Level.INFO, "SUCCESS")
        return Response(true, "success")
    }

    suspend fun stop(call: ApplicationCall): Response {
        val id = call.parameters["id"] ?: throw Exception("id is required")
        val newEndedAt = DateUtil.stopParkingFormatter.format(Date().toInstant())
        val stopParkingRequest = StopParkingRequest(newEndedAt = newEndedAt)
        Api.patch(call, "/v1/ssp/parking_session/${id}/edit") {
            setBody(stopParkingRequest)
        }
        Metrics.logAndCount(call, Method.Stop, Level.INFO, "SUCCESS")
        return Response(true, "success")
    }

    suspend fun history(call: ApplicationCall): HistoryResponse {
        val parkingSessions = retrieveParkingSessions(call)

        val completed = parkingSessions.filter { it.status == "COMPLETED" }.map(this::convertParkingSession)

        Metrics.logAndCount(call, Method.History, Level.INFO, "SUCCESS")
        return HistoryResponse(completed)
    }

}
