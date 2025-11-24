package nl.parkeerassistent.route

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import nl.parkeerassistent.module
import nl.parkeerassistent.service.ParkingMeter
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GeoRoutesKtTest {

    @Test
    fun testGetParkingmeters() = testApplication {
        application {
            module()
        }
        val response = client.get("/geo/parking-meters/nearby") {
            url {
                parameters.append("lat", "52.3536628")
                parameters.append("lon", "4.92935411")
            }
        }
        assert(response.status.value == 200)
        val body = response.bodyAsText()
        val parkingMeters = Json.decodeFromString<List<ParkingMeter>>(body)
        assertEquals(25, parkingMeters.size)
        assertEquals("ZACHARIAS JANSESTRAAT 36 T/H", parkingMeters[2].name)
    }

}