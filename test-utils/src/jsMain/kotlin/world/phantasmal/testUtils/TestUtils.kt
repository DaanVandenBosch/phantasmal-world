package world.phantasmal.testUtils

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

internal actual fun asyncTest(block: suspend () -> Unit): dynamic =
    GlobalScope.promise { block() }

internal actual fun canExecuteSlowTests(): Boolean = false
