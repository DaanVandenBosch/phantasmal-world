package world.phantasmal.testUtils

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

internal actual fun testAsync(block: suspend () -> Unit): dynamic =
    GlobalScope.promise { block() }

internal actual fun canExecuteSlowTests(): Boolean = false
