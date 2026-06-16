package nl.parkeerassistent.route

import io.ktor.client.request.cookie
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
            cookie("token", "XXX")
        }
        assert(response.status.value == 200)
        val body = response.bodyAsText()
        val parkingMeters = Json.decodeFromString<List<ParkingMeter>>(body)
        assertEquals(25, parkingMeters.size)
        assertEquals("ZACHARIAS JANSESTRAAT 36 T/H", parkingMeters[2].name)
    }

    @Test
    fun `test fetching a parking meter by its id`() = testApplication {
        application {
            module()
        }
        val response = client.get("/geo/parking-meters/15505") {
            cookie("token", "XXX")
        }
        assert(response.status.value == 200)
        val body = response.bodyAsText()
        val parkingMeter = Json.decodeFromString<ParkingMeter>(body)
        assertEquals(15505, parkingMeter.id)
    }

    @Test
    fun `test fetching a parking meter that doesn't exist`() = testApplication {
        application {
            module()
        }
        val response = client.get("/geo/parking-meters/99999") {
            cookie("token", "XXX")
        }
        assert(response.status.value == 404)
    }


}