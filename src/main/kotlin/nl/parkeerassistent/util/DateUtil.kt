package nl.parkeerassistent.util

import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

object DateUtil {

    val amsterdam = TimeZone.getTimeZone("Europe/Amsterdam")

    val dateTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX")
    val date = SimpleDateFormat("yyyy-MM-dd")

    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX").withZone(amsterdam.toZoneId())
    val rfc1123Formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("E, d MMM yyyy HH:mm:ss 'GMT'").withZone(ZoneId.from(ZoneOffset.UTC))
    val stopParkingFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.000+00:00").withZone(ZoneId.from(ZoneOffset.UTC))

    fun dateWithTime(date: Date, time: String): String {
        val calendar = Calendar.getInstance()
        calendar.time = date

        val hour = time.substring(0, 2).toInt()
        val minutes = time.substring(3, 5).toInt()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minutes)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return dateTime.format(calendar.time)
    }

    fun nextDayOfWeekAt(
        targetDay: String,
        targetTime: String
    ): ZonedDateTime {
        val now: ZonedDateTime = ZonedDateTime.now(amsterdam.toZoneId())

        val hour = targetTime.take(2).toInt()
        val minute = targetTime.substring(3, 5).toInt()

        val targetTime = LocalTime.of(hour, minute)

        // First: move to the correct day
        val dow = java.time.DayOfWeek.entries.first { it.name.startsWith(targetDay) }
        var result = now.with(TemporalAdjusters.nextOrSame(dow))

        // Then set the time
        result = result.with(targetTime)

        // If "today at HH:MM" is already in the past → jump to *next week's* target day
        if (result <= now) {
            result = result.plusWeeks(1)
        }

        return result
    }

}

enum class DayOfWeek {
    MON,
    TUE,
    WED,
    THU,
    FRI,
    SAT,
    SUN,
    ;
}