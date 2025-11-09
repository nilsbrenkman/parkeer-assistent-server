package nl.parkeerassistent.util

import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

object DateUtil {

    val amsterdam = TimeZone.getTimeZone("Europe/Amsterdam")

    val dateTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX")
    val date = SimpleDateFormat("yyyy-MM-dd")

    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX").withZone(amsterdam.toZoneId())
    val gmtDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneId.from(ZoneOffset.UTC))
    val esIndexFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMM").withZone(ZoneId.from(ZoneOffset.UTC))

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

}
