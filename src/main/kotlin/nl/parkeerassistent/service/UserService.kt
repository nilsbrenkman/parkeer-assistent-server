package nl.parkeerassistent.service

import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.server.plugins.BadRequestException
import nl.parkeerassistent.Metrics
import nl.parkeerassistent.client.CloudApi
import nl.parkeerassistent.client.Session
import nl.parkeerassistent.client.ensureData
import nl.parkeerassistent.external.Permit
import nl.parkeerassistent.external.Permits
import nl.parkeerassistent.model.BalanceResponse
import nl.parkeerassistent.model.Regime
import nl.parkeerassistent.model.RegimeDay
import nl.parkeerassistent.model.RegimeResponse
import nl.parkeerassistent.model.UserResponse
import nl.parkeerassistent.util.DateUtil
import org.slf4j.event.Level
import java.time.Instant
import java.util.Calendar
import java.util.Date

object UserService {

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

    suspend fun get(session: Session): UserResponse {

        val permits = getPermits(session)

        if (permits.permits.size != 1) {
            Metrics.logAndCount(session.call, Method.Get, Level.WARN, "PERMIT_NOT_ONE")
            throw BadRequestException("Response contained wrong number of permits [permits=${permits.permits.size}]")
        }
        val permit = permits.permits.first()

        if (session.permit?.paymentZoneId == null) {
            session.permit = nl.parkeerassistent.client.Permit(permit.reportCode, permit.paymentZones.first().id)
        }

        val regime = getRegime(permit, Instant.now())
        val fullRegime = getFullRegime(permit)

        Metrics.logAndCount(session.call, Method.Get, Level.INFO, "SUCCESS")
        return UserResponse(formatBalance(permits), permit.parkingRate.value, regime.regimeTimeStart, regime.regimeTimeEnd, fullRegime)
    }

    suspend fun balance(session: Session): BalanceResponse {
        val permits = getPermits(session)

        Metrics.logAndCount(session.call, Method.Balance, Level.INFO, "SUCCESS")
        return BalanceResponse(formatBalance(permits))
    }

    private fun formatBalance(permits: Permits) = "%.2f".format(permits.wallet.balance)

    suspend fun regime(session: Session): RegimeResponse {
        val regimeDate = ensureData(session.call.parameters["date"], "date")

        val permits = getPermits(session)

        val regime = getRegime(permits.permits.first(), DateUtil.date.parse(regimeDate).toInstant())

        Metrics.logAndCount(session.call, Method.Regime, Level.INFO, "SUCCESS")
        return regime
    }

    private suspend fun getPermits(session: Session): Permits {
        return CloudApi.get(session, "v1/permits") {
            parameter("status", "Actief")
        }.body()
    }

    private fun getRegime(permit: Permit, date: Instant): RegimeResponse {
        val calendar = Calendar.getInstance()
        calendar.time = Date.from(date)
        val dayOfWeek = DayOfWeek.values().get(calendar.get(Calendar.DAY_OF_WEEK) - 1)
        val day = permit.paymentZones.first().days.first{ it.dayOfWeek == dayOfWeek.alias }
        val startTime = DateUtil.dateWithTime(calendar.time, day.startTime)
        val endTime = DateUtil.dateWithTime(calendar.time, day.endTime)
        return RegimeResponse(startTime, endTime)
    }

    private fun getFullRegime(permit: Permit): Regime {
        val paymentZone = permit.paymentZones.first()
        val days = paymentZone.days
            .filter { d -> ! ("00:00" == d.startTime && "24:00" == d.endTime && !paymentZone.description.contains("ma-zo 00-24")) }
            .mapNotNull { d ->
                val dayOfWeek = DayOfWeek.fromAlias(d.dayOfWeek)
                dayOfWeek?.let { RegimeDay(
                    it.name,
                    if ("00:00" == d.startTime) "00:01" else d.startTime,
                    if ("24:00" == d.endTime) "23:59" else d.endTime
                ) }
            }
        return Regime(days)
    }

    enum class DayOfWeek(val alias: String) {
        SUN("Zondag"),
        MON("Maandag"),
        TUE("Dinsdag"),
        WED("Woensdag"),
        THU("Donderdag"),
        FRI("Vrijdag"),
        SAT("Zaterdag"),
        ;
        companion object {
            fun fromAlias(a: String): DayOfWeek? {
                for (d in DayOfWeek.values()) {
                    if (d.alias == a) {
                        return d
                    }
                }
                return null
            }
        }
    }

}