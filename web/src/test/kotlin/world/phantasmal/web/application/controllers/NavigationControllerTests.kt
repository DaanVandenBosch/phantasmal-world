package world.phantasmal.web.application.controllers

import kotlinx.datetime.Instant
import world.phantasmal.web.test.StubClock
import world.phantasmal.web.test.WebTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigationControllerTests : WebTestSuite() {
    @Test
    fun internet_time_is_calculated_correctly() = test {
        val clock = StubClock()
        components.clock = clock

        listOf(
            Pair("00:00:00", 41),
            Pair("13:10:12", 590),
            Pair("22:59:59", 999),
            Pair("23:00:00", 0),
            Pair("23:59:59", 41),
        ).forEach { (time, beats) ->
            clock.currentTime = Instant.parse("2020-01-01T${time}Z")
            val ctrl = NavigationController(components.uiStore, components.clock)

            assertEquals("@$beats", ctrl.internetTime.value)
        }
    }
}
