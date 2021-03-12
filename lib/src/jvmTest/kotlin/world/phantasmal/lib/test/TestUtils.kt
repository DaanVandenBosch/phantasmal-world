@file:JvmName("TestUtilsJvm")

package world.phantasmal.lib.test

import world.phantasmal.lib.buffer.Buffer
import world.phantasmal.lib.cursor.Cursor
import world.phantasmal.lib.cursor.cursor

actual suspend fun readFile(path: String): Cursor =
    Buffer.fromResource(path).cursor()
