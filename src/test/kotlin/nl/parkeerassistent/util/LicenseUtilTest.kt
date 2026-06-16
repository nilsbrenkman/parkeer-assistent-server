package nl.parkeerassistent.util

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LicenseUtilTest {

    @Test
    fun `normalise uppercases and strips all non-alphanumeric characters`() {
        assertEquals("AB12CD", LicenseUtil.normalise("ab-12-cd"))
        assertEquals("1ABC23", LicenseUtil.normalise("1a bc.23"))
        assertEquals("AB12CD", LicenseUtil.normalise("AB12CD"))
        assertEquals("XY99ZZ", LicenseUtil.normalise(" xy/99.zz! "))
    }

    @Test
    fun `format applies the two-two-two pattern`() {
        assertEquals("AB-12-CD", LicenseUtil.format("AB12CD"))
    }

    @Test
    fun `format applies the one-three-two pattern`() {
        assertEquals("1-ABC-23", LicenseUtil.format("1ABC23"))
    }

    @Test
    fun `format applies the two-three-one pattern`() {
        assertEquals("12-ABC-3", LicenseUtil.format("12ABC3"))
    }

    @Test
    fun `format applies the three-two-one pattern`() {
        assertEquals("ABC-12-3", LicenseUtil.format("ABC123"))
    }

    @Test
    fun `format applies the one-two-three pattern`() {
        assertEquals("1-AB-234", LicenseUtil.format("1AB234"))
    }

    @Test
    fun `format normalises separators before matching`() {
        assertEquals("AB-12-CD", LicenseUtil.format("ab-12-cd"))
    }

    @Test
    fun `format returns the normalised value when no pattern matches`() {
        // Wrong length: none of the six-character patterns apply.
        assertEquals("ABCDE", LicenseUtil.format("abcde"))
    }
}
