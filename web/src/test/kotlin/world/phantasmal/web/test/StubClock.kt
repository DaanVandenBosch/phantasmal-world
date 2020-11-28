package world.phantasmal.web.test

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class StubClock(var currentTime: Instant = Instant.DISTANT_PAST) : Clock {
    override fun now(): Instant = currentTime
}
