package nl.parkeerassistent.service

import io.ktor.client.call.body
import io.ktor.client.request.setBody
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import nl.parkeerassistent.Metrics
import nl.parkeerassistent.client.Api
import nl.parkeerassistent.external.ListVrns
import nl.parkeerassistent.external.Vrn
import nl.parkeerassistent.model.AddVisitorRequest
import nl.parkeerassistent.model.Response
import nl.parkeerassistent.model.Visitor
import nl.parkeerassistent.model.VisitorResponse
import nl.parkeerassistent.util.LicenseUtil
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

/**
 * Service responsible for the user's saved visitors (favorite license plates).
 *
 * Visitors are stored upstream as "favorite VRNs" (Vehicle Registration
 * Numbers). This service lists, adds and deletes them, normalizing license
 * plates on the way in via [LicenseUtil.normalise] and formatting them for
 * display via [LicenseUtil.format] on the way out. All operations delegate to
 * the upstream SSP (Self-Service Portal) API through [Api] and emit metrics
 * via [Metrics].
 */
object VisitorService {

    private val LOG = LoggerFactory.getLogger(VisitorService::class.java)

    /**
     * Enumeration of service methods exposed by [VisitorService].
     *
     * Used by [Metrics] to tag log entries and counters with a consistent
     * service/method pair.
     */
    enum class Method: ServiceMethod {
        /** List the saved visitors. */
        Get,
        /** Add a new saved visitor. */
        Add,
        /** Delete a saved visitor. */
        Delete,
        ;
        override fun service(): String {
            return "Visitor"
        }
        override fun method(): String {
            return name
        }
    }

    /**
     * Retrieves the user's saved visitors.
     *
     * Each upstream favorite VRN is mapped to a [Visitor], exposing both the
     * raw license plate and a display-formatted variant.
     *
     * @param call the incoming Ktor application call.
     * @return a [VisitorResponse] containing the saved visitors.
     */
    suspend fun get(call: ApplicationCall): VisitorResponse {
        val response = Api.get(call, "/v1/ssp/favorite_vrn/list")
        val vrnList = response.body<ListVrns>().vrns

        Metrics.logAndCount(call, Method.Get, Level.INFO, "SUCCESS")
        return VisitorResponse(vrnList.map {
            Visitor(
                id = it.id ?: 0,
                license = it.vrn,
                formattedLicense = LicenseUtil.format(it.vrn),
                name = it.description
            )
        })
    }

    /**
     * Adds a new saved visitor.
     *
     * The request body is parsed as an [AddVisitorRequest]; the license plate
     * is normalized via [LicenseUtil.normalise] before being stored upstream
     * as a favorite VRN with the given name as its description.
     *
     * @param call the incoming Ktor application call carrying the
     *             [AddVisitorRequest] body.
     * @return a successful [Response] when the upstream call completes.
     */
    suspend fun add(call: ApplicationCall): Response {
        val request = call.receive<AddVisitorRequest>()
        val vrn = Vrn(
            vrn = LicenseUtil.normalise(request.license),
            description = request.name,
        )
        Api.post(call, "/v1/ssp/favorite_vrn/add") {
            setBody(vrn)
        }
        Metrics.logAndCount(call, Method.Add, Level.INFO, "SUCCESS")
        return Response(true, "success")
    }

    /**
     * Deletes a saved visitor.
     *
     * The visitor identifier is read from the `id` path/query parameter of the
     * [call].
     *
     * @param call the incoming Ktor application call; the `id` parameter must
     *             be present.
     * @return a successful [Response] when the upstream call completes.
     * @throws Exception if the `id` parameter is missing.
     */
    suspend fun delete(call: ApplicationCall): Response {
        val id = call.parameters["id"]?.toLong() ?: throw Exception("id is required")
        Api.delete(call, "/v1/ssp/favorite_vrn/$id/delete")
        Metrics.logAndCount(call, Method.Delete, Level.INFO, "SUCCESS")
        return Response(true, "success")
    }

}