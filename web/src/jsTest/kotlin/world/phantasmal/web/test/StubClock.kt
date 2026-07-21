package world.phantasmal.web.test

import world.phantasmal.web.core.Clock

class StubClock(var currentTimeMillis: Double = 0.0) : Clock {
    override fun nowMillis(): Double = currentTimeMillis
}
