package world.phantasmal.psolib.test

import kotlinx.browser.window
import kotlinx.coroutines.await
import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.cursor.ArrayBufferCursor
import world.phantasmal.psolib.cursor.Cursor

actual suspend fun readFile(path: String): Cursor {
    return window.fetch(path)
        .then {
            require(it.ok) { """Couldn't load resource "$path".""" }
            it.arrayBuffer()
        }
        .then { ArrayBufferCursor(it, Endianness.Little) }
        .await()
}
