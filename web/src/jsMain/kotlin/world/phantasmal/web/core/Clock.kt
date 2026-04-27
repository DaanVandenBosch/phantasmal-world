package world.phantasmal.web.core

import kotlin.js.Date

/**
 * A thin wrapper around the system clock that can be stubbed in tests.
 * Returns the current Unix epoch time in milliseconds.
 *
 * Replaces kotlinx-datetime's [kotlinx.datetime.Clock] — the only use of
 * kotlinx-datetime in this module was reading the current wall-clock time to
 * compute Swatch Internet Time, which does not justify bundling js-joda.
 */
interface Clock {
    fun nowMillis(): Double

    object System : Clock {
        override fun nowMillis(): Double = Date.now()
    }
}
