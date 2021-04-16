package world.phantasmal.web.core.files

import kotlinx.coroutines.await
import world.phantasmal.core.use
import world.phantasmal.lib.Endianness
import world.phantasmal.lib.buffer.Buffer
import world.phantasmal.lib.cursor.Cursor
import world.phantasmal.lib.cursor.cursor
import world.phantasmal.webui.files.FileHandle

suspend fun FileHandle.cursor(endianness: Endianness): Cursor =
    arrayBuffer().cursor(endianness)

suspend fun FileHandle.System.writeBuffer(buffer: Buffer) {
    writableStream().use { it.write(buffer.arrayBuffer).await() }
}
