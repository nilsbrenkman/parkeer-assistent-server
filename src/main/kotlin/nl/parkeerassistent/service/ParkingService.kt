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
import nl.parkeerassistent.service.ParkingService.retrieveParkingSessions
import nl.parkeerassistent.util.DateUtil
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date

/**
 * Service responsible for managing parking sessions.
 *
 * Provides operations for retrieving active and scheduled parking sessions,
 * starting a new parking session, stopping a running session, and fetching
 * the history of completed sessions. All operations delegate to the upstream
 * SSP (Self-Service Portal) API through [Api] and emit metrics via [Metrics].
 */
object ParkingService {

    private val LOG = LoggerFactory.getLogger(ParkingService::class.java)

    /**
     * Enumeration of service methods exposed by [ParkingService].
     *
     * Used by [Metrics] to tag log entries and counters with a consistent
     * service/method pair.
     */
    enum class Method: ServiceMethod {
        /** Retrieve active and scheduled parking sessions. */
        Get,
        /** Start a new parking session. */
        Start,
        /** Stop a currently running parking session. */
        Stop,
        /** Retrieve completed (historical) parking sessions. */
        History,
        ;
        override fun service(): String {
            return "Parking"
        }
        override fun method(): String {
            return name
        }
    }

    /**
     * Retrieves the active and scheduled parking sessions for the current user.
     *
     * The session list is filtered by the `product_id` cookie present on the
     * incoming [call]. Sessions with status `ACTIVE` are returned as active
     * entries, while sessions with status `FUTURE` are returned as scheduled.
     *
     * @param call the incoming Ktor application call, used for authentication
     *             cookies and metrics tagging.
     * @return a [ParkingResponse] containing the active and scheduled sessions.
     * @throws Exception if the `product_id` cookie is missing.
     */
    suspend fun get(call: ApplicationCall): ParkingResponse {
        val parkingSessions = retrieveParkingSessions(call)

        val active = parkingSessions
            .filter { it.status == "ACTIVE" }
            .map(this::convertParkingSession)
            .sortedBy { it.startTime }
        val scheduled = parkingSessions
            .filter { it.status == "FUTURE" }
            .map(this::convertParkingSession)
            .sortedBy { it.startTime }

        Metrics.logAndCount(call, Method.Get, Level.INFO, "SUCCESS")
        return ParkingResponse(active, scheduled)
    }

    /**
     * Fetches the paginated list of parking sessions for the product associated
     * with the current request.
     *
     * Up to 100 sessions are requested, sorted by `parking_session_id` in
     * descending order. The list contains sessions in all statuses; callers
     * are expected to filter the result as needed.
     *
     * @param call the incoming Ktor application call; the `product_id` cookie
     *             must be present.
     * @return the list of [ParkingSession] entries returned by the upstream API.
     * @throws Exception if the `product_id` cookie is missing.
     */
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

    /**
     * Converts an upstream [ParkingSession] DTO into the public [Parking] model.
     *
     * The cost is converted from cents (as returned by the API) to a decimal
     * amount in the major currency unit, and start/end times are formatted
     * with [DateUtil.dateFormatter].
     */
    private fun convertParkingSession(parkingSession: ParkingSession): Parking = Parking(
        id = parkingSession.id,
        license = parkingSession.vrn,
        startTime = convertTime(parkingSession.startDate),
        endTime = convertTime(parkingSession.endDate),
        cost = parkingSession.cost / 100.0,
    )

    /**
     * Parses an ISO-8601 offset date-time string and formats it using
     * [DateUtil.dateFormatter].
     *
     * @param gmt the ISO-8601 offset date-time string returned by the API.
     * @return the date-time formatted for client consumption.
     */
    private fun convertTime(gmt: String): String {
        val date = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(gmt)
        return DateUtil.dateFormatter.format(date)
    }

    /**
     * Starts a new parking session.
     *
     * The request body is parsed as an [AddParkingRequest]. The session start
     * time is taken from the request (or the current time if not provided),
     * shifted forward by one second to avoid clock-skew rejections on the
     * upstream side, and the end time is computed by adding
     * [AddParkingRequest.timeMinutes] minutes. The resulting session is
     * registered with the upstream API and marked as a new favorite.
     *
     * @param call the incoming Ktor application call carrying the
     *             [AddParkingRequest] body.
     * @return a successful [Response] when the upstream call completes.
     */
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

    /**
     * Stops a running parking session by patching its end time to the current
     * instant.
     *
     * The session identifier is read from the `id` path/query parameter of the
     * [call]. The new end time is formatted with [DateUtil.stopParkingFormatter].
     *
     * @param call the incoming Ktor application call; the `id` parameter must
     *             be present.
     * @return a successful [Response] when the upstream call completes.
     * @throws Exception if the `id` parameter is missing.
     */
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

    /**
     * Retrieves the history of completed parking sessions for the current user.
     *
     * Reuses [retrieveParkingSessions] and keeps only entries with status
     * `COMPLETED`.
     *
     * @param call the incoming Ktor application call; the `product_id` cookie
     *             must be present.
     * @return a [HistoryResponse] containing the completed sessions.
     * @throws Exception if the `product_id` cookie is missing.
     */
    suspend fun history(call: ApplicationCall): HistoryResponse {
        val parkingSessions = retrieveParkingSessions(call)

        val completed = parkingSessions.filter { it.status == "COMPLETED" }.map(this::convertParkingSession)

        Metrics.logAndCount(call, Method.History, Level.INFO, "SUCCESS")
        return HistoryResponse(completed)
    }

}
