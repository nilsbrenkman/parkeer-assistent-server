package nl.parkeerassistent.util

import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DateUtilTest {

    @Test
    fun `test date formatting rfc1123`() {
        val date = DateUtil.dateTime.parse("2025-11-15T20:18:25+01:00")
        assertEquals("Sat, 15 Nov 2025 19:18:25 GMT", DateUtil.rfc1123Formatter.format(date.toInstant()))
    }

    @Test
    fun `test date formatting stop parking`() {
        val date = DateUtil.dateTime.parse("2025-11-15T20:18:25+01:00")
        assertEquals("2025-11-15T19:18:25.000+00:00", DateUtil.stopParkingFormatter.format(date.toInstant()))
    }

    @Test
    fun `nextDayOfWeekAt lands on the requested weekday and time in the future`() {
        // Invariants hold regardless of when the test runs: the result is in the
        // Amsterdam zone, matches the requested weekday and time-of-day, and is
        // strictly after "now".
        val now = ZonedDateTime.now(DateUtil.amsterdam.toZoneId())
        val result = DateUtil.nextDayOfWeekAt("WED", "09:30")

        assertEquals(DateUtil.amsterdam.toZoneId(), result.zone)
        assertEquals(DayOfWeek.WEDNESDAY, result.dayOfWeek)
        assertEquals(9, result.hour)
        assertEquals(30, result.minute)
        assertEquals(0, result.second)
        assertTrue(result.isAfter(now), "expected $result to be after $now")
        // The next matching weekday is always within a week from now.
        assertTrue(result.isBefore(now.plusWeeks(1).plusDays(1)))
    }

    @Test
    fun `nextDayOfWeekAt matches weekday prefixes for every day`() {
        val days = mapOf(
            "MON" to DayOfWeek.MONDAY,
            "TUE" to DayOfWeek.TUESDAY,
            "WED" to DayOfWeek.WEDNESDAY,
            "THU" to DayOfWeek.THURSDAY,
            "FRI" to DayOfWeek.FRIDAY,
            "SAT" to DayOfWeek.SATURDAY,
            "SUN" to DayOfWeek.SUNDAY,
        )
        days.forEach { (prefix, expected) ->
            assertEquals(expected, DateUtil.nextDayOfWeekAt(prefix, "12:00").dayOfWeek)
        }
    }
}