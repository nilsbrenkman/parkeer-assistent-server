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
import nl.parkeerassistent.service.UserService.balance
import nl.parkeerassistent.service.UserService.get
import nl.parkeerassistent.service.UserService.getEndTime
import nl.parkeerassistent.service.UserService.regime
import nl.parkeerassistent.util.DateUtil
import nl.parkeerassistent.util.DayOfWeek
import org.slf4j.event.Level
import kotlin.time.Duration.Companion.hours

/**
 * Service responsible for the current user's parking product and its context.
 *
 * Resolves the active visitor/scratch permit product, derives the applicable
 * parking zone, paid-parking regime (the weekly schedule of paid hours) and
 * hourly rate, and reports the wallet balance. The active product id is cached
 * in the `product_id` cookie by [get] and is required by [balance] and
 * [regime] as well as by [ParkingService]. All operations delegate to the
 * upstream SSP (Self-Service Portal) API through [Api] and emit metrics via
 * [Metrics].
 */
object UserService {

    /**
     * Enumeration of service methods exposed by [UserService].
     *
     * Used by [Metrics] to tag log entries and counters with a consistent
     * service/method pair.
     */
    enum class Method : ServiceMethod {
        /** Resolve the active product and its zone, regime, rate and balance. */
        Get,
        /** Retrieve the current wallet balance. */
        Balance,
        /** Retrieve the zone, regime and rate for a given parking meter. */
        Regime,
        ;

        override fun service(): String {
            return "User"
        }

        override fun method(): String {
            return name
        }
    }

    /**
     * Resolves the active parking product and all context the client needs.
     *
     * Finds the active visitor/scratch product, stores its id in the
     * `product_id` cookie, then derives the parking zone, regime, hourly rate
     * and wallet balance for that product's default parking meter. The
     * `product_id` cookie set here is a prerequisite for [balance], [regime]
     * and the parking endpoints.
     *
     * @param call the incoming Ktor application call; the `product_id` cookie
     *             is written to its response.
     * @return a [UserResponse] with the balance, hourly rate, product id, zone
     *         id, parking-meter id and regime.
     * @throws BadRequestException if no active visitor/scratch product exists.
     */
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
        // The account may have no favorite parking meter (upstream returns
        // `favorite_machine_number: null`); without one there is no default
        // zone/regime/rate to resolve, so leave them unset.
        val parkingMeterId = productDetails.ssp.parkingMeterId
        if (parkingMeterId == null) {
            Metrics.logAndCount(call, Method.Get, Level.WARN, "NO_PARKING_METER")
            return UserResponse(
                balance = formatBalance(productDetails.ssp.mainAccount.moneyBalance ?: 0),
                hourRate = 0.1,
                productId = product.id,
                zoneId = 0,
                parkingMeterId = 0,
                regime = Regime(
                    days = DayOfWeek.entries.map {
                        RegimeDay(weekday = it.name, startTime = "00:00", endTime = "23:59")
                    }
                ),
            )
        }
        val (zoneId, regime) = getParkingZone(call, product.id, parkingMeterId)
        val hourRate = getHourRate(call, product.id, parkingMeterId, regime)

        Metrics.logAndCount(call, Method.Get, Level.INFO, "SUCCESS")
        return UserResponse(
            balance = formatBalance(productDetails.ssp.mainAccount.moneyBalance ?: 0),
            hourRate = hourRate,
            productId = product.id,
            zoneId = zoneId,
            parkingMeterId = parkingMeterId,
            regime = regime,
        )
    }

    /**
     * Retrieves the current wallet balance for the active product.
     *
     * @param call the incoming Ktor application call; the `product_id` cookie
     *             (set by [get]) must be present.
     * @return a [BalanceResponse] with the formatted balance.
     * @throws Exception if the `product_id` cookie is missing.
     */
    suspend fun balance(call: ApplicationCall): BalanceResponse {
        val productId = call.request.cookies["product_id"]?.toLong() ?: throw Exception("product_id is required")
        val productDetails = getClientProduct(call, productId)

        Metrics.logAndCount(call, Method.Balance, Level.INFO, "SUCCESS")
        return BalanceResponse(
            balance = formatBalance(productDetails.ssp.mainAccount.moneyBalance ?: 0),
        )
    }

    /**
     * Resolves the zone, paid-parking regime and hourly rate for a specific
     * parking meter.
     *
     * Used when the client picks a parking meter other than the product's
     * default, so the applicable cost and paid hours can be shown before
     * starting a session. The product is taken from the `product_id` cookie
     * and the parking meter from the `id` path/query parameter.
     *
     * @param call the incoming Ktor application call; the `product_id` cookie
     *             and the `id` parameter must both be present.
     * @return a [RegimeResponse] with the hourly rate, zone id and regime.
     * @throws Exception if the `product_id` cookie or the `id` parameter is
     *         missing.
     */
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

    /**
     * Fetches the user's product list and returns the first active
     * visitor/scratch permit, or `null` if none exists.
     */
    private suspend fun getProduct(call: ApplicationCall): Product? {
        val response = Api.get(call, "/v1/permit_overview/product_list") {
            parameter("page", "1")
            parameter("row_per_page", "100")
        }
        val products = response.body<Paginated<Product>>().data
        return products.firstOrNull { it.status == "ACTIVE" && (it.permitType == "visitor" || it.permitType == "scratch") }
    }

    /**
     * Fetches the detailed [ClientProduct] (including balance and default
     * parking meter) for the given product id.
     */
    private suspend fun getClientProduct(call: ApplicationCall, id: Long): ClientProduct {
        val response = Api.get(call, "/v1/client_product/$id")
        val clientProduct = response.body<ClientProduct>()
        return clientProduct
    }

    /**
     * Resolves the parking zone for a product/parking-meter combination and
     * builds its weekly [Regime].
     *
     * The upstream zone exposes paid-parking time frames per weekday; the first
     * frame of each day is mapped to a [RegimeDay], normalizing the end time
     * via [getEndTime] and formatting both bounds as `HH:mm`.
     *
     * @return a pair of the zone id and the constructed [Regime].
     */
    private suspend fun getParkingZone(call: ApplicationCall, productId: Long, parkingMeterId: Long): Pair<Long, Regime> {
        val request = ParkingZoneRequest(
            productId = productId,
            parkingMeterId = parkingMeterId
        )
        val response = Api.post(call, "/v1/ssp/paid_parking_zone/get_by_machine_number") {
            setBody(request)
        }
        val parkingZone = response.body<ParkingZoneResponse>()
        val zoneDays = parkingZone.zone.timeFrameData
        val regimeDays = mutableListOf<RegimeDay>()
        DayOfWeek.entries.forEach { day ->
            val zoneDay = zoneDays[day.ordinal]
            zoneDay.firstOrNull()?.let { zoneTime ->
                regimeDays.add(RegimeDay(
                    weekday = day.name,
                    startTime = formatTimeHHMM(zoneTime.startTime ?: 0),
                    endTime = formatTimeHHMM(getEndTime(zoneTime.endTime)),
                ))
            }
        }
        return parkingZone.zone.zoneId to Regime(regimeDays)
    }

    /**
     * Normalizes an upstream end-of-regime time, mapping a `null` or
     * end-of-day `2400` value to `2359`.
     */
    fun getEndTime(endTime: Long?): Long {
        if (endTime == null || endTime == 2400L) return 2359
        return endTime
    }

    /**
     * Derives the hourly parking rate by asking the upstream cost calculator
     * for the price of a one-hour session.
     *
     * Starts from the next occurrence of the first regime day's start time and
     * requests a cost estimate; the rate is the calculated cost divided by the
     * calculated time, converted from cents to a per-hour decimal amount. If
     * the estimate is zero (e.g. the chosen hour turns out to be free) it
     * retries one week later, up to three attempts, returning `0.0` if no
     * positive cost is found.
     *
     * @return the hourly rate in the major currency unit, or `0.0` if it could
     *         not be determined.
     */
    private suspend fun getHourRate(call: ApplicationCall, productId: Long, parkingMeterId: Long, regime: Regime): Double {
        val regimeDay = regime.days.first()
        var startTime = DateUtil.nextDayOfWeekAt(regimeDay.weekday, regimeDay.startTime)
        var retry = 0
        do {
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
            if (estimate.calculatedCost > 0L) {
                return estimate.calculatedCost / (estimate.calculatedTime / 1.hours.inWholeSeconds.toDouble()) / 100
            }
            startTime = startTime.plusWeeks(1)
            retry++
        } while (retry < 3)
        return 0.0
    }

    /** Formats a balance given in cents as a two-decimal amount, e.g. `1250` -> `"12.50"`. */
    private fun formatBalance(balance: Long) = "%.2f".format(balance / 100.0)

    /**
     * Formats an integer-encoded time of day as `HH:mm`, zero-padding to four
     * digits first, e.g. `900` -> `"09:00"`.
     */
    private fun formatTimeHHMM(time: Long): String {
        // Normalize to 4 digits, e.g. 900 -> "0900"
        val padded = time.toString().padStart(4, '0')
        val hours = padded.take(2)
        val minutes = padded.substring(2, 4)
        return "$hours:$minutes"
    }

}