@file:JvmName("AsyncTestJvm")

package world.phantasmal.testUtils

import kotlinx.coroutines.runBlocking

internal actual fun testAsync(block: suspend () -> Unit) {
    runBlocking { block() }
}

internal actual fun canExecuteSlowTests(): Boolean = true
