package nl.parkeerassistent.mock

import org.slf4j.LoggerFactory
import java.time.Instant

object MockStateContainer {

    val log = LoggerFactory.getLogger(MockStateContainer::class.java)

    private var mockState = MockState()

    fun mock(): MockState {
        if (mockState.expiresAt < Instant.now()) {
            log.info("Mock expired")
            mockState = MockState()
        }
        return mockState
    }

    fun reset() {
        log.info("Resetting mock")
        mockState = MockState()
    }

}