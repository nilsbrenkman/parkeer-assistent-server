package nl.parkeerassistent.mock

import nl.parkeerassistent.model.AddParkingRequest
import nl.parkeerassistent.model.Parking
import nl.parkeerassistent.model.PaymentRequest
import nl.parkeerassistent.model.PaymentResponse
import nl.parkeerassistent.model.Regime
import nl.parkeerassistent.model.RegimeDay
import nl.parkeerassistent.model.StatusResponse
import nl.parkeerassistent.model.Visitor
import nl.parkeerassistent.util.DateUtil
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

class MockState {

    companion object {
        private val log = LoggerFactory.getLogger(MockState::class.java)

        val hourRate = 2.1
    }

    val idGenerator = IdGenerator()

    val expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES)

    var user: User
    val balance get() = paymentList.values.sumOf(Payment::balance) - parkingList.sumOf(Parking::cost)

    private var visitorList: MutableList<Visitor>
    val visitors get() = visitorList.map(Visitor::convert)

    private var parkingList: MutableList<Parking>
    val active get() = parkingList
        .filter { p -> p.start.before(Date()) && p.end.after(Date()) }
        .sortedBy { p -> p.start }
        .map(Parking::convert)
    val scheduled get() = parkingList
        .filter { p -> p.start.after(Date()) }
        .sortedBy { p -> p.start }
        .map(Parking::convert)
    val history get() = parkingList
        .filter { p -> p.end.before(Date()) }
        .sortedByDescending { p -> p.start }
        .map(Parking::convert)

    private var paymentList: MutableMap<String, Payment> = HashMap()

    init {
        log.info("Creating MockState")
        user = User(false)

        startPayment(PaymentRequest(2500, "SUCCESS", "NL"))

        visitorList = mutableListOf(
            Visitor(idGenerator.getId(), "111-AA-1", "Suzanne"),
            Visitor(idGenerator.getId(), "22-BBB-2", "Erik"),
        )
        parkingList = mutableListOf()

        startParking(AddParkingRequest(visitorList[0].license, 15, Date().addingMinutes(-14), 1, 2, 55105))
        startParking(AddParkingRequest(visitorList[1].license, 60, Date().addingMinutes(2), 1, 2, 55105))
        startParking(AddParkingRequest(visitorList[0].license, 60, Date().addingMinutes(-2 * 60), 1, 2, 55105))
    }

    fun startParking(request: AddParkingRequest) {
        val calendar = Calendar.getInstance()
        calendar.time = request.start?.let { start -> DateUtil.dateTime.parse(start) } ?: Date()
        calendar.add(Calendar.SECOND, 1)
        val start = calendar.time
        calendar.add(Calendar.MINUTE, request.timeMinutes)
        val end = calendar.time

        val parking = Parking(
            idGenerator.getId(),
            request.license,
            "",
            start,
            end
        )
        if (parking.cost() <= balance) {
            parkingList.add(parking)
        }
    }

    fun stopParking(id: Long) {
        val parking = parkingList.find { p -> p.id == id }

        if (parking == null) {
            return
        }
        parking.end = Date()
        if (parking.end < parking.start) {
            parkingList.remove(parking)
        }
    }

    fun addVisitor(name: String, license: String) {
        visitorList.add(Visitor(idGenerator.getId(), license, name))
    }

    fun deleteVisitor(id: Long) {
        visitorList.removeIf { visitor -> visitor.id == id }
    }

    fun startPayment(request: PaymentRequest): PaymentResponse {
        val uuid = UUID.randomUUID().toString()
        val amount = request.amount.toDouble() / 100
        val payment = Payment(uuid, request.brand, amount, Instant.now())
        paymentList[uuid] = payment

        return PaymentResponse("https://parkeerassistent.nl/completeMockPayment?id=$uuid")
    }

    fun checkPayment(transactionId: String): StatusResponse {
        val payment = paymentList[transactionId] ?: return StatusResponse("error")
        return StatusResponse(payment.status)
    }

    class User(var loggedIn: Boolean) {

        val regime = Regime(
            listOf(
                RegimeDay("MON", "09:00", "21:00"),
                RegimeDay("TUE", "09:00", "21:00"),
                RegimeDay("WED", "09:00", "21:00"),
                RegimeDay("THU", "09:00", "21:00"),
                RegimeDay("FRI", "09:00", "21:00"),
                RegimeDay("SAT", "12:00", "17:00"),
            )
        )

        val regimeStart get() = regimeForDate(Date()).first
        val regimeEnd get() = regimeForDate(Date()).second

        fun regimeForDate(date: Date): Pair<String, String> {
            val sdf = SimpleDateFormat("E", Locale.ENGLISH)
            val weekday = sdf.format(date).uppercase()

            val regimeDay = regime.days.find { day -> day.weekday == weekday }
            val startTime = regimeDay?.startTime ?: "00:00"
            val endTime = regimeDay?.endTime ?: "00:00"

            val startDate = DateUtil.dateWithTime(date, startTime)
            val endDate = DateUtil.dateWithTime(date, endTime)

            return startDate to endDate
        }

    }

    class Visitor(val id: Long,
                  val license: String,
                  val name: String?) {

        fun convert(): nl.parkeerassistent.model.Visitor {
            return Visitor(id, license, license, name)
        }
    }

    class Parking(
        val id: Long,
        val license: String,
        val name: String?,
        var start: Date,
        var end: Date
    ) {

        fun cost(): Double {
            val diffInHours = (end.time - start.time) / 1_000 / 60.0 / 60.0
            return "%.2f".format(diffInHours * hourRate).toDouble()
        }

        fun convert(): nl.parkeerassistent.model.Parking {
            return Parking(
                id, license, name, DateUtil.dateTime.format(start), DateUtil.dateTime.format(end), cost()
            )
        }
    }

    data class Payment(val id: String, val issuer: String, val amount: Double, val createdAt: Instant) {
        val status: String
            get() {
                return when (issuer) {
                    "SUCCESS" -> "success"
                    "PENDING" -> "pending"
                    "PENDING10" -> {
                        if (Instant.now() > createdAt.plusSeconds(10)) {
                            return "success"
                        }
                        return "pending"
                    }
                    "ERROR" -> "pending"
                    else -> "unknown"
                }
            }
        val balance: Double
            get() {
                return if (status == "success") amount else 0.0
            }
    }

    class IdGenerator {
        private var counter = AtomicLong(0)
        fun getId(): Long {
            return counter.getAndAdd(1)
        }
    }

}

fun Date.addingMinutes(minutes: Int): String {
    val calendar = Calendar.getInstance(DateUtil.amsterdam)
    calendar.time = this
    calendar.add(Calendar.MINUTE, minutes)
    return DateUtil.dateTime.format(calendar.time)
}
