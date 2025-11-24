package nl.parkeerassistent.service

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.LocalDate
import kotlin.math.cos

object GeoService {

    private val LOG = LoggerFactory.getLogger(GeoService::class.java)

    enum class Method : ServiceMethod {
        ParkingMeters,
        ;

        override fun service(): String {
            return "Geo"
        }

        override fun method(): String {
            return name
        }
    }

    private val parkingMeters by lazy {
        val json = object {}.javaClass.getResource("/parkeerautomaten.json")?.readText()
            ?: throw IllegalStateException("parkeerautomaten.json not found in resources")

        val featureCollection = Json.decodeFromString<FeatureCollection>(json)

        featureCollection.features.map {
            val (lon, lat) = it.geometry.coordinates.first().toPoint()
            val (name, type) = ParkingMeterType.detect(it.properties.oms)

            ParkingMeter(
                id = it.properties.id,
                domein = it.properties.domeinId,
                name = name,
                type = type,
                start = LocalDate.parse(it.properties.startDate),
                end = it.properties.endDate?.let { end -> LocalDate.parse(end) },
                latitude = lat,
                longitude = lon,
                point = Point.from(latitude = lat, longitude = lon),
                distance = null,
            )
        }.filter { it.end == null || it.end >= LocalDate.now() }
    }

    fun getParkingMetersNearby(point: Point, n: Int): List<ParkingMeter> {
        return parkingMeters
            .map { it.copy(distance = distance(point, it.point!!)) }
            .sortedBy { it.distance }
            .take(n)
    }

    fun getParkingMetersInRegion(a: Point, b: Point): List<ParkingMeter> {
        val middle = Point((a.x + b.x) / 2, (a.y + b.y) / 2)
        return parkingMeters
            .filter { it.point != null
                    && it.point.x >= a.x.coerceAtMost(b.x)
                    && it.point.x <= a.x.coerceAtLeast(b.x)
                    && it.point.y >= a.y.coerceAtMost(b.y)
                    && it.point.y <= a.y.coerceAtLeast(b.y)
            }
            .map { it.copy(distance = distance(middle, it.point!!)) }
            .sortedBy { it.distance }
    }

    private fun distance(a: Point, b: Point): Double {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return dx * dx + dy * dy
    }

}

@Serializable
data class ParkingMeter(
    val id: Int,
    val domein: Int,
    val name: String,
    val type: ParkingMeterType,
    @Transient val start: LocalDate? = null,
    @Transient val end: LocalDate? = null,
    val latitude: Double,
    val longitude: Double,
    @Transient val point: Point? = null,
    val distance: Double?,
)

enum class ParkingMeterType(private val suffix: String) {
    SIGN("_BORD"),
    METER(""),
    ;

    companion object {
        fun detect(oms: String): Pair<String, ParkingMeterType> {
            for (value in entries) {
                if (oms.endsWith(value.suffix)) {
                    return Pair(oms.removeSuffix(value.suffix).trim(), value)
                }
            }
            throw Exception("Not possible")
        }
    }
}

@Serializable
data class FeatureCollection(
    val type: String,
    val name: String,
    val crs: Crs,
    val features: List<Feature>
)

@Serializable
data class Crs(
    val type: String,
    val properties: CrsProperties
)

@Serializable
data class CrsProperties(
    val name: String
)

@Serializable
data class Feature(
    val type: String,
    val properties: FeatureProperties,
    val geometry: Geometry
)

@Serializable
data class FeatureProperties(
    @SerialName("DOMEIN_COD") val domeinId: Int,
    @SerialName("VERKOOP_PU") val id: Int,
    @SerialName("OMS") val oms: String,
    @SerialName("B_DAT_VERK") val startDate: String,
    @SerialName("E_DAT_VERK") val endDate: String?
)

@Serializable
data class Geometry(
    val type: String,
    val coordinates: List<List<Double>>
)

@Serializable
data class Point(val x: Double, val y: Double) {
    companion object {
        private val R = 6371000.0
        fun from(latitude: Double, longitude: Double): Point {
            return Point(
                x = Math.toRadians(longitude) * R * cos(x = Math.toRadians(latitude)),
                y = Math.toRadians(latitude) * R
            )
        }
    }
}

fun List<Double>.toPoint() = Point(this.get(0), this.get(1))