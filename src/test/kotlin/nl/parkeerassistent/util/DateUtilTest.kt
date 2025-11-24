package nl.parkeerassistent.util

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

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

}