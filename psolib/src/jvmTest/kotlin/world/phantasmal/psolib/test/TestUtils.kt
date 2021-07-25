@file:JvmName("TestUtilsJvm")

package world.phantasmal.psolib.test

import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.Cursor
import world.phantasmal.psolib.cursor.cursor

actual suspend fun readFile(path: String): Cursor =
    Buffer.fromResource(path).cursor()
