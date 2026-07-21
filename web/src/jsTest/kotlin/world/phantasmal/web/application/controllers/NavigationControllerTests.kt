package world.phantasmal.web.application.controllers

import world.phantasmal.web.test.StubClock
import world.phantasmal.web.test.WebTestSuite
import kotlin.js.Date
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigationControllerTests : WebTestSuite {
    @Test
    fun internet_time_is_calculated_correctly() = test {
        val clock = StubClock()
        components.clock = clock

        listOf(
            Triple(0, 0, 0) to 41,
            Triple(13, 10, 12) to 590,
            Triple(22, 59, 59) to 999,
            Triple(23, 0, 0) to 0,
            Triple(23, 59, 59) to 41,
        ).forEach { (hms, beats) ->
            val (h, m, s) = hms
            // 2020-01-01T${hh:mm:ss}Z
            clock.currentTimeMillis = Date.UTC(
                year = 2020,
                month = 0,
                day = 1,
                hour = h,
                minute = m,
                second = s,
            )
            val ctrl = disposer.add(NavigationController(components.uiStore, components.clock))

            assertEquals("@$beats", ctrl.internetTime.value)
        }
    }
}
