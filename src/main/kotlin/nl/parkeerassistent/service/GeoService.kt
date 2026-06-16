package nl.parkeerassistent.service

import io.ktor.server.plugins.NotFoundException
import io.ktor.server.routing.RoutingCall
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import nl.parkeerassistent.service.GeoService.getParkingMetersInRegion
import nl.parkeerassistent.service.GeoService.getParkingMetersNearby
import org.slf4j.LoggerFactory
import java.time.LocalDate
import kotlin.math.cos

/**
 * Service for locating Amsterdam parking meters.
 *
 * Unlike the other services this one does not call the upstream API: it loads a
 * bundled GeoJSON dataset (`parkeerautomaten.json`) of parking meters, projects
 * each meter's longitude/latitude onto a planar coordinate system in meters
 * (see [Point.from]) and answers spatial queries — nearest neighbors to a point
 * ([getParkingMetersNearby]) and all meters within a rectangular region
 * ([getParkingMetersInRegion]).
 */
object GeoService {

    private val LOG = LoggerFactory.getLogger(GeoService::class.java)

    /**
     * Enumeration of service methods exposed by [GeoService].
     *
     * Used by [nl.parkeerassistent.Metrics] to tag log entries and counters
     * with a consistent service/method pair.
     */
    enum class Method : ServiceMethod {
        /** Look up parking meters by location. */
        ParkingMeters,
        ;

        override fun service(): String {
            return "Geo"
        }

        override fun method(): String {
            return name
        }
    }

    /**
     * The parking meters, parsed lazily from the bundled `parkeerautomaten.json`
     * resource on first access.
     *
     * Each GeoJSON feature is mapped to a [ParkingMeter] with its coordinates
     * projected to planar meters; meters whose sale period has already ended
     * (`end` before today) are filtered out. The list is computed once and
     * reused for all queries.
     */
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

    /**
     * Returns the [n] parking meters closest to [point], nearest first.
     *
     * Each returned meter has its `distance` populated relative to [point].
     *
     * @param point the reference location in planar (projected) coordinates.
     * @param n the maximum number of meters to return.
     * @return up to [n] parking meters sorted by ascending distance.
     */
    fun getParkingMetersNearby(point: Point, n: Int): List<ParkingMeter> {
        return parkingMeters
            .map { it.copy(distance = distance(point, it.point!!)) }
            .sortedBy { it.distance }
            .take(n)
    }

    /**
     * Returns all parking meters within the axis-aligned rectangle whose
     * opposite corners are [a] and [b].
     *
     * The corners may be given in any order. Each returned meter has its
     * `distance` populated relative to the rectangle's center, and the result
     * is sorted by ascending distance from that center.
     *
     * @param a one corner of the region, in planar (projected) coordinates.
     * @param b the opposite corner of the region.
     * @return the parking meters inside the region, nearest to the center
     *         first.
     */
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

    /**
     * Returns the squared Euclidean distance between [a] and [b].
     *
     * The square root is intentionally omitted: only relative ordering matters
     * for the nearest/region queries, and skipping it is cheaper.
     */
    private fun distance(a: Point, b: Point): Double {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return dx * dx + dy * dy
    }

    fun details(call: RoutingCall): ParkingMeter {
        val id = call.parameters["id"]?.toInt() ?: throw Exception("id is required")
        val parkingMeter = parkingMeters.firstOrNull { it.id == id } ?: throw NotFoundException("parking meter not found")
        return parkingMeter.copy(distance = 0.0)
    }

    fun load() {
        LOG.info("Loading parking meters: ${parkingMeters.size}")
    }
}

/**
 * A single parking meter exposed to clients.
 *
 * `start`/`end` (the sale period) and `point` (the projected planar
 * coordinates) are used only server-side and are excluded from serialization
 * via [Transient]. `distance` is populated per query relative to the query's
 * reference location.
 */
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

/**
 * The kind of parking meter, inferred from the suffix of its GeoJSON
 * description: a roadside sign ([SIGN], suffix `_BORD`) or a physical pay
 * machine ([METER], no suffix).
 */
enum class ParkingMeterType(private val suffix: String) {
    SIGN("_BORD"),
    METER(""),
    ;

    companion object {
        /**
         * Splits a GeoJSON `OMS` description into its display name and meter
         * type by matching the type suffix.
         *
         * @return a pair of the cleaned name and the detected type.
         * @throws Exception if no known type suffix matches (not expected, as
         *         [METER] has an empty suffix that always matches).
         */
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

/** Root of the GeoJSON `parkeerautomaten.json` dataset: a collection of [Feature]s. */
@Serializable
data class FeatureCollection(
    val type: String,
    val name: String,
    val crs: Crs,
    val features: List<Feature>
)

/** GeoJSON coordinate reference system descriptor (carried through, not used for projection). */
@Serializable
data class Crs(
    val type: String,
    val properties: CrsProperties
)

/** Properties of a GeoJSON [Crs], holding its name. */
@Serializable
data class CrsProperties(
    val name: String
)

/** A single GeoJSON feature: one parking meter's [FeatureProperties] and [Geometry]. */
@Serializable
data class Feature(
    val type: String,
    val properties: FeatureProperties,
    val geometry: Geometry
)

/**
 * The attribute fields of a parking-meter GeoJSON feature.
 *
 * The [SerialName] annotations map the dataset's Dutch column names to readable
 * properties: `DOMEIN_COD` (domain id), `VERKOOP_PU` (meter id), `OMS`
 * (description), and the sale period bounds `B_DAT_VERK`/`E_DAT_VERK`.
 */
@Serializable
data class FeatureProperties(
    @SerialName("DOMEIN_COD") val domeinId: Int,
    @SerialName("VERKOOP_PU") val id: Int,
    @SerialName("OMS") val oms: String,
    @SerialName("B_DAT_VERK") val startDate: String,
    @SerialName("E_DAT_VERK") val endDate: String?
)

/** GeoJSON geometry; coordinates are `[longitude, latitude]` pairs. */
@Serializable
data class Geometry(
    val type: String,
    val coordinates: List<List<Double>>
)

/**
 * A 2D point in planar coordinates (meters), used for distance calculations.
 */
@Serializable
data class Point(val x: Double, val y: Double) {
    companion object {
        /** Mean Earth radius in meters, used for the equirectangular projection. */
        private val R = 6371000.0

        /**
         * Projects geographic [latitude]/[longitude] (degrees) onto planar
         * coordinates in meters using an equirectangular projection.
         *
         * Suitable for the small, fixed extent of Amsterdam where the
         * distortion is negligible and only relative distances are needed.
         */
        fun from(latitude: Double, longitude: Double): Point {
            return Point(
                x = Math.toRadians(longitude) * R * cos(x = Math.toRadians(latitude)),
                y = Math.toRadians(latitude) * R
            )
        }
    }
}

/** Reads a GeoJSON `[longitude, latitude]` coordinate pair into a [Point]. */
fun List<Double>.toPoint() = Point(this.get(0), this.get(1))