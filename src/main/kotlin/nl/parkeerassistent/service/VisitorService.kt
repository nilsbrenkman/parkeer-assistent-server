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

object VisitorService {

    private val LOG = LoggerFactory.getLogger(VisitorService::class.java)

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

    suspend fun delete(call: ApplicationCall): Response {
        val id = call.parameters["id"]?.toLong() ?: throw Exception("id is required")
        Api.delete(call, "/v1/ssp/favorite_vrn/$id/delete")
        Metrics.logAndCount(call, Method.Delete, Level.INFO, "SUCCESS")
        return Response(true, "success")
    }

}