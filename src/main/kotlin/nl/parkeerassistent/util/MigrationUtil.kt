package nl.parkeerassistent.util

import kotlin.math.abs

object MigrationUtil {

    fun createId(string: String): Int {
        return abs(string.hashCode() % Int.MAX_VALUE)
    }

}