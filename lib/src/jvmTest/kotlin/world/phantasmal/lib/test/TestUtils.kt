@file:JvmName("TestUtilsJvm")

package world.phantasmal.lib.test

import world.phantasmal.lib.buffer.Buffer
import world.phantasmal.lib.cursor.Cursor
import world.phantasmal.lib.cursor.cursor

actual suspend fun readFile(path: String): Cursor {
    val stream = {}::class.java.getResourceAsStream(path)
        ?: error("""Couldn't load resource "$path".""")

    stream.use {
        @Suppress("BlockingMethodInNonBlockingContext")
        return Buffer.fromByteArray(it.readAllBytes()).cursor()
    }
}
