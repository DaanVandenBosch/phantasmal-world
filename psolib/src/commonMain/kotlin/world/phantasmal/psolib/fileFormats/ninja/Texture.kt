package world.phantasmal.psolib.fileFormats.ninja

import mu.KotlinLogging
import world.phantasmal.core.Failure
import world.phantasmal.core.PwResult
import world.phantasmal.core.Severity
import world.phantasmal.core.Success
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.Cursor
import world.phantasmal.psolib.fileFormats.parseIff
import world.phantasmal.psolib.fileFormats.parseIffHeaders

private val logger = KotlinLogging.logger {}

private const val XVMH = 0x484d5658
private const val XVRT = 0x54525658

class Xvm(
    val textures: List<XvrTexture>,
)

class XvrTexture(
    val id: Int,
    val format: Pair<Int, Int>,
    val width: Int,
    val height: Int,
    val size: Int,
    val data: Buffer,
)

fun parseXvr(cursor: Cursor): XvrTexture {
    val format1 = cursor.int()
    val format2 = cursor.int()
    val id = cursor.int()
    val width = cursor.uShort().toInt()
    val height = cursor.uShort().toInt()
    val size = cursor.int()
    cursor.seek(36)
    val data = cursor.buffer(size)
    return XvrTexture(
        id,
        format = Pair(format1, format2),
        width,
        height,
        size,
        data,
    )
}

fun isXvm(cursor: Cursor): Boolean {
    val iffResult = parseIffHeaders(cursor, silent = true)
    cursor.seekStart(0)

    return iffResult is Success &&
            iffResult.value.any { chunk -> chunk.type == XVMH || chunk.type == XVRT }
}

fun parseXvm(cursor: Cursor): PwResult<Xvm> {
    val iffResult = parseIff(cursor)

    if (iffResult !is Success) {
        return iffResult as Failure
    }

    val result = PwResult.build<Xvm>(logger)
    result.addResult(iffResult)
    val chunks = iffResult.value
    val headerChunk = chunks.find { it.type == XVMH }
    val header = headerChunk?.data?.let(::parseHeader)

    val textures = chunks
        .filter { it.type == XVRT }
        .map { parseXvr(it.data) }

    if (header == null && textures.isEmpty()) {
        result.addProblem(
            Severity.Error,
            "Corrupted XVM file.",
            "No header and no XVRT chunks found.",
        )

        return result.failure()
    }

    if (header != null && header.textureCount != textures.size) {
        result.addProblem(
            Severity.Warning,
            "Corrupted XVM file.",
            "Found ${textures.size} textures instead of ${header.textureCount} as defined in the header.",
        )
    }

    return result.success(Xvm(textures))
}

private class Header(
    val textureCount: Int,
)

private fun parseHeader(cursor: Cursor): Header {
    val textureCount = cursor.uShort().toInt()
    return Header(textureCount)
}
