package world.phantasmal.testUtils

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

actual fun asyncTest(block: suspend () -> Unit): dynamic = GlobalScope.promise { block() }
