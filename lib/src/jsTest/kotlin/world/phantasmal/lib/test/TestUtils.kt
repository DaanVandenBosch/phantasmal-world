package world.phantasmal.lib.test

import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.await
import kotlinx.coroutines.promise
import world.phantasmal.lib.Endianness
import world.phantasmal.lib.cursor.ArrayBufferCursor
import world.phantasmal.lib.cursor.Cursor

actual fun asyncTest(block: suspend () -> Unit): dynamic = GlobalScope.promise { block() }

actual suspend fun readFile(path: String): Cursor {
    return window.fetch(path)
        .then {
            require(it.ok) { """Couldn't load resource "$path".""" }
            it.arrayBuffer()
        }
        .then { ArrayBufferCursor(it, Endianness.Little) }
        .await()
}
