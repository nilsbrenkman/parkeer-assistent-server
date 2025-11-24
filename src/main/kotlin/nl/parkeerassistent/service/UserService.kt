package nl.parkeerassistent.service

import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.CookieEncoding
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import nl.parkeerassistent.Metrics
import nl.parkeerassistent.client.Api
import nl.parkeerassistent.external.ClientProduct
import nl.parkeerassistent.external.CostEstimateRequest
import nl.parkeerassistent.external.CostEstimateResponse
import nl.parkeerassistent.external.Paginated
import nl.parkeerassistent.external.ParkingZoneRequest
import nl.parkeerassistent.external.ParkingZoneResponse
import nl.parkeerassistent.external.Product
import nl.parkeerassistent.model.BalanceResponse
import nl.parkeerassistent.model.Regime
import nl.parkeerassistent.model.RegimeDay
import nl.parkeerassistent.model.RegimeResponse
import nl.parkeerassistent.model.UserResponse
import nl.parkeerassistent.util.DateUtil
import nl.parkeerassistent.util.DayOfWeek
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import kotlin.time.Duration.Companion.hours

object UserService {

    private val LOG = LoggerFactory.getLogger(UserService::class.java)

    enum class Method : ServiceMethod {
        Get,
        Balance,
        Regime,
        ;

        override fun service(): String {
            return "User"
        }

        override fun method(): String {
            return name
        }
    }

    suspend fun get(call: ApplicationCall): UserResponse {
        val product = getProduct(call)
        if (product == null) {
            Metrics.logAndCount(call, Method.Get, Level.WARN, "PRODUCT_NOT_FOUND")
            throw BadRequestException("Product not found")
        }
        call.response.cookies.append(
            name = "product_id",
            value = product.id.toString(),
            encoding = CookieEncoding.DQUOTES,
            httpOnly = true,
            secure = false,
        )
        val productDetails = getClientProduct(call, product.id)
        val (zoneId, regime) = getParkingZone(call, product.id, productDetails.ssp.parkingMeterId)
        val hourRate = getHourRate(call, product.id, productDetails.ssp.parkingMeterId, regime)

        Metrics.logAndCount(call, Method.Get, Level.INFO, "SUCCESS")
        return UserResponse(
            balance = formatBalance(productDetails.ssp.mainAccount.moneyBalance ?: 0),
            hourRate = hourRate,
            productId = product.id,
            zoneId = zoneId,
            parkingMeterId = productDetails.ssp.parkingMeterId,
            regime = regime,
        )
    }

    suspend fun balance(call: ApplicationCall): BalanceResponse {
        val productId = call.request.cookies["product_id"]?.toLong() ?: throw Exception("product_id is required")
        val productDetails = getClientProduct(call, productId)

        Metrics.logAndCount(call, Method.Balance, Level.INFO, "SUCCESS")
        return BalanceResponse(
            balance = formatBalance(productDetails.ssp.mainAccount.moneyBalance ?: 0),
        )
    }

    suspend fun regime(call: ApplicationCall): RegimeResponse {
        val productId = call.request.cookies["product_id"]?.toLong() ?: throw Exception("product_id is required")
        val parkingMeterId = call.parameters["id"]?.toLong() ?: throw Exception("id is required")
        val (zoneId, regime) = getParkingZone(call, productId, parkingMeterId)
        val hourRate = getHourRate(call, productId, parkingMeterId, regime)
        Metrics.logAndCount(call, Method.Regime, Level.INFO, "SUCCESS")
        return RegimeResponse(
            hourRate = hourRate,
            zoneId = zoneId,
            regime = regime,
        )
    }

    private suspend fun getProduct(call: ApplicationCall): Product? {
        val response = Api.get(call, "/v1/permit_overview/product_list") {
            parameter("page", "1")
            parameter("row_per_page", "50")
        }
        val products = response.body<Paginated<Product>>().data
        return products.firstOrNull { it.status == "ACTIVE" && it.permitType == "visitor" }
    }

    private suspend fun getClientProduct(call: ApplicationCall, id: Long): ClientProduct {
        val response = Api.get(call, "/v1/client_product/$id")
        val clientProduct = response.body<ClientProduct>()
        return clientProduct
    }

    private suspend fun getParkingZone(call: ApplicationCall, productId: Long, parkingMeterId: Long): Pair<Long, Regime> {
        val request = ParkingZoneRequest(
            productId = productId,
            parkingMeterId = parkingMeterId
        )
        val response = Api.post(call, "/v1/ssp/paid_parking_zone/get_by_machine_number") {
            setBody(request)
        }
        val parkingZone = response.body<ParkingZoneResponse>()
        val zoneDays = parkingZone.zone.timeFrameDate
        val regimeDays = mutableListOf<RegimeDay>()
        DayOfWeek.entries.forEach { day ->
            val zoneDay = zoneDays[day.ordinal]
            zoneDay.firstOrNull()?.let { zoneTime ->
                regimeDays.add(RegimeDay(
                    weekday = day.name,
                    startTime = formatTimeHHMM(zoneTime.startTime ?: 0),
                    endTime = formatTimeHHMM(zoneTime.endTime ?: 2359),
                ))
            }
        }
        return parkingZone.zone.zoneId to Regime(regimeDays)
    }

    private suspend fun getHourRate(call: ApplicationCall, productId: Long, parkingMeterId: Long, regime: Regime): Double {
        val regimeDay = regime.days.first()
        val startTime = DateUtil.nextDayOfWeekAt(regimeDay.weekday, regimeDay.startTime)
        val endTime = startTime.plusHours(1)
        val request = CostEstimateRequest(
            productId = productId,
            startedAt = DateUtil.rfc1123Formatter.format(startTime.toInstant()),
            endedAt = DateUtil.rfc1123Formatter.format(endTime.toInstant()),
            parkingMeterId = parkingMeterId,
        )
        val response = Api.post(call, "/v1/ssp/parking_session/cost_calculator") {
            setBody(request)
        }
        val estimate = response.body<CostEstimateResponse>().parkingSessionBalance
        return estimate.calculatedCost / (estimate.calculatedTime / 1.hours.inWholeSeconds.toDouble()) / 100
    }

    private fun formatBalance(balance: Long) = "%.2f".format(balance / 100.0)

    private fun formatTimeHHMM(time: Long): String {
        // Normalize to 4 digits, e.g. 900 -> "0900"
        val padded = time.toString().padStart(4, '0')
        val hours = padded.take(2)
        val minutes = padded.substring(2, 4)
        return "$hours:$minutes"
    }

}