package world.phantasmal.web.core.files

import world.phantasmal.lib.Endianness
import world.phantasmal.lib.cursor.Cursor
import world.phantasmal.lib.cursor.cursor
import world.phantasmal.webui.files.FileHandle

suspend fun FileHandle.cursor(endianness: Endianness): Cursor =
    arrayBuffer().cursor(endianness)
