package nl.parkeerassistent.service

import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import nl.parkeerassistent.Metrics
import nl.parkeerassistent.client.CloudApi
import nl.parkeerassistent.client.Session
import nl.parkeerassistent.client.ensureData
import nl.parkeerassistent.external.LicensePlate
import nl.parkeerassistent.model.AddVisitorRequest
import nl.parkeerassistent.model.Response
import nl.parkeerassistent.model.Visitor
import nl.parkeerassistent.model.VisitorResponse
import nl.parkeerassistent.util.LicenseUtil
import nl.parkeerassistent.util.MigrationUtil
import nl.parkeerassistent.util.ServiceUtil
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

object VisitorService {

    private val log = LoggerFactory.getLogger(VisitorService::class.java)

    enum class Method: ServiceMethod {
        Get,
        Add,
        Delete,
        ;
        override fun service(): String {
            return "Visitor"
        }
        override fun method(): String {
            return name
        }
    }

    suspend fun get(session: Session): VisitorResponse {
        val reportCode = ensureData(session.permit?.reportCode, "report code")

        val licensePlates: List<LicensePlate> = CloudApi.get(session, "v1/licenseplates") {
            parameter("reportCode", reportCode)
        }.body()

        Metrics.logAndCount(session.call, Method.Get, Level.INFO, "SUCCESS")
        return VisitorResponse(licensePlates.map {
            Visitor(
                MigrationUtil.createId(it.vehicleId),
                reportCode,
                it.vehicleId,
                LicenseUtil.format(it.vehicleId),
                it.visitorName
            )
        })
    }

    suspend fun add(session: Session, request: AddVisitorRequest): Response {
        val licensePlate = LicensePlate(LicenseUtil.normalise(request.license), request.name, session.permit?.reportCode)
        CloudApi.post(session, "v1/licenseplates") {
            setBody(licensePlate)
        }
        return ServiceUtil.convertResponse(session.call, Method.Add, true)
    }

    suspend fun delete(session: Session): Response {
        val id = ensureData(session.call.parameters["visitorId"]?.toInt(),"visitor id")

        val visitors = get(session).visitors
        val visitor = ensureData(visitors.firstOrNull{ v -> v.visitorId == id }, "visitor found")

        val licensePlate = LicensePlate(visitor.license, visitor.name, session.permit?.reportCode)

        val result: String = CloudApi.delete(session, "v1/licenseplates") {
            setBody(licensePlate)
        }.body()

        return ServiceUtil.convertResponse(session.call, Method.Delete, result == "OK")
    }

}