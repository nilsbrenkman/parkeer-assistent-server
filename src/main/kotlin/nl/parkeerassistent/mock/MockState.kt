package nl.parkeerassistent.mock

import kotlinx.serialization.Serializable
import nl.parkeerassistent.model.AddParkingRequest
import nl.parkeerassistent.model.Parking
import nl.parkeerassistent.model.PaymentRequest
import nl.parkeerassistent.model.PaymentResponse
import nl.parkeerassistent.model.Regime
import nl.parkeerassistent.model.RegimeDay
import nl.parkeerassistent.model.Visitor
import nl.parkeerassistent.util.DateUtil
import nl.parkeerassistent.util.LicenseUtil
import java.util.Calendar
import java.util.Date
import java.util.UUID

@Serializable
class MockState(
    private var idCounter: Long = 0,
    private val visitors: MutableList<MockVisitor> = mutableListOf(),
    private val parkings: MutableList<MockParking> = mutableListOf(),
    private val payments: MutableList<MockPayment> = mutableListOf(),
) {

    val balance: Double
        get() = payments.sumOf { it.balance } - parkings.sumOf { it.cost() }

    val visitorList: List<Visitor>
        get() = visitors.map(MockVisitor::toModel)

    val active: List<Parking>
        get() = parkings
            .filter { Date(it.start).before(Date()) && Date(it.end).after(Date()) }
            .sortedBy { it.start }
            .map(MockParking::toModel)

    val scheduled: List<Parking>
        get() = parkings
            .filter { Date(it.start).after(Date()) }
            .sortedBy { it.start }
            .map(MockParking::toModel)

    val history: List<Parking>
        get() = parkings
            .filter { Date(it.end).before(Date()) }
            .sortedByDescending { it.start }
            .map(MockParking::toModel)

    private fun nextId(): Long = idCounter++

    fun addVisitor(name: String, license: String): MockVisitor {
        val visitor = MockVisitor(nextId(), license, name)
        visitors.add(visitor)
        return visitor
    }

    fun deleteVisitor(id: Long) {
        visitors.removeIf { it.id == id }
    }

    fun startParking(request: AddParkingRequest) {
        val calendar = Calendar.getInstance()
        calendar.time = request.start?.let { start -> DateUtil.dateTime.parse(start) } ?: Date()
        calendar.add(Calendar.SECOND, 1)
        val start = calendar.time
        calendar.add(Calendar.MINUTE, request.timeMinutes)
        val end = calendar.time

        val parking = MockParking(nextId(), request.license, null, start.time, end.time)
        if (parking.cost() <= balance) {
            parkings.add(parking)
        }
    }

    fun stopParking(id: Long) {
        val index = parkings.indexOfFirst { it.id == id }
        if (index < 0) {
            return
        }
        val parking = parkings[index]
        val now = Date().time
        if (now < parking.start) {
            parkings.removeAt(index)
        } else {
            parkings[index] = parking.copy(end = now)
        }
    }

    fun startPayment(request: PaymentRequest): PaymentResponse {
        val uuid = UUID.randomUUID().toString()
        val amount = request.amount.toDouble() / 100
        payments.add(MockPayment(uuid, request.brand, amount, System.currentTimeMillis()))

        return PaymentResponse("https://parkeerassistent.nl/completeMockPayment?id=$uuid")
    }

    companion object {
        const val HOUR_RATE = 2.1

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

        fun dummy(): MockState {
            val state = MockState()
            state.startPayment(PaymentRequest(2500, "SUCCESS", "NL"))

            val suzanne = state.addVisitor("Suzanne", "111AA1")
            val erik = state.addVisitor("Erik", "22BBB2")

            state.startParking(AddParkingRequest(suzanne.license, 15, Date().addingMinutes(-14), 1, 2, 55105))
            state.startParking(AddParkingRequest(erik.license, 60, Date().addingMinutes(2), 1, 2, 55105))
            state.startParking(AddParkingRequest(suzanne.license, 60, Date().addingMinutes(-2 * 60), 1, 2, 55105))
            return state
        }
    }
}

@Serializable
data class MockVisitor(
    val id: Long,
    val license: String,
    val name: String? = null,
) {
    fun toModel(): Visitor = Visitor(id, license, LicenseUtil.format(license), name)
}

@Serializable
data class MockParking(
    val id: Long,
    val license: String,
    val name: String? = null,
    val start: Long,
    val end: Long,
) {
    fun cost(): Double {
        val diffInHours = (end - start) / 1_000 / 60.0 / 60.0
        return "%.2f".format(diffInHours * MockState.HOUR_RATE).toDouble()
    }

    fun toModel(): Parking = Parking(
        id = id,
        license = license,
        name = name,
        startTime = DateUtil.dateTime.format(Date(start)),
        endTime = DateUtil.dateTime.format(Date(end)),
        cost = cost(),
    )
}

@Serializable
data class MockPayment(
    val id: String,
    val issuer: String,
    val amount: Double,
    val createdAt: Long,
) {
    val status: String
        get() = when (issuer) {
            "SUCCESS" -> "success"
            "PENDING" -> "pending"
            "PENDING10" -> if (System.currentTimeMillis() > createdAt + 10_000) "success" else "pending"
            "ERROR" -> "pending"
            else -> "unknown"
        }

    val balance: Double
        get() = if (status == "success") amount else 0.0
}

fun Date.addingMinutes(minutes: Int): String {
    val calendar = Calendar.getInstance(DateUtil.amsterdam)
    calendar.time = this
    calendar.add(Calendar.MINUTE, minutes)
    return DateUtil.dateTime.format(calendar.time)
}
