package world.phantasmal.testUtils

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

// We need to return the promise here while still letting the type checker think we're not returning
// anything, i.e. returning Unit. The Mocha JS test framework needs this promise to be able to wait
// for test completion. This is a giant hack, and at this point we should probably switch to using
// the kotlinx-coroutines-test library.
internal actual fun testAsync(block: suspend () -> Unit): Unit =
    @Suppress("UnsafeCastFromDynamic")
    @OptIn(DelicateCoroutinesApi::class)
    GlobalScope.promise { block() }.asDynamic()

// KJS is relatively slow, so we don't execute the slow tests on KJS.
internal actual fun canExecuteSlowTests(): Boolean = false
