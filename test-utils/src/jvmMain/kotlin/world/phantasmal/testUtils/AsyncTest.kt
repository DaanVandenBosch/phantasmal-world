@file:JvmName("AsyncTestJvm")

package world.phantasmal.testUtils

import kotlinx.coroutines.runBlocking

actual fun asyncTest(block: suspend () -> Unit) {
    runBlocking { block() }
}
