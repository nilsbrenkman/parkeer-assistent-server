package nl.parkeerassistent.service

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class UserServiceTest {

    @Test
    fun `getEndTime maps null to end of day`() {
        assertEquals(2359, UserService.getEndTime(null))
    }

    @Test
    fun `getEndTime maps 2400 to 2359`() {
        assertEquals(2359, UserService.getEndTime(2400L))
    }

    @Test
    fun `getEndTime passes other values through`() {
        assertEquals(2100, UserService.getEndTime(2100L))
        assertEquals(900, UserService.getEndTime(900L))
    }
}
