package world.phantasmal.lib.fileFormats.quest

import mu.KotlinLogging
import world.phantasmal.core.PwResult
import world.phantasmal.core.Severity
import world.phantasmal.core.Success
import world.phantasmal.core.filenameBase
import world.phantasmal.lib.Endianness
import world.phantasmal.lib.buffer.Buffer
import world.phantasmal.lib.cursor.Cursor
import world.phantasmal.lib.cursor.WritableCursor
import world.phantasmal.lib.cursor.cursor
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

private val logger = KotlinLogging.logger {}

// .qst format
private const val DC_GC_PC_HEADER_SIZE = 60
private const val BB_HEADER_SIZE = 88
private const val ONLINE_QUEST = 0x44
private const val DOWNLOAD_QUEST = 0xa6

// Chunks
private const val CHUNK_BODY_SIZE = 1024
private const val DC_GC_PC_CHUNK_HEADER_SIZE = 20
private const val DC_GC_PC_CHUNK_TRAILER_SIZE = 4
private const val DC_GC_PC_CHUNK_SIZE =
    CHUNK_BODY_SIZE + DC_GC_PC_CHUNK_HEADER_SIZE + DC_GC_PC_CHUNK_TRAILER_SIZE
private const val BB_CHUNK_HEADER_SIZE = 24
private const val BB_CHUNK_TRAILER_SIZE = 8
private const val BB_CHUNK_SIZE = CHUNK_BODY_SIZE + BB_CHUNK_HEADER_SIZE + BB_CHUNK_TRAILER_SIZE

class QstContent(
    val version: Version,
    val online: Boolean,
    val files: List<QstContainedFile>,
)

class QstContainedFile(
    val id: Int?,
    val filename: String,
    val questName: String?,
    val data: Buffer,
)

/**
 * Low level parsing function for .qst files.
 */
fun parseQst(cursor: Cursor): PwResult<QstContent> {
    val result = PwResult.build<QstContent>(logger)

    // A .qst file contains two headers that describe the embedded .dat and .bin files.
    // Read headers and contained files.
    val headers = parseHeaders(cursor)

    if (headers.size < 2) {
        return result
            .addProblem(
                Severity.Error,
                "This .qst file is corrupt.",
                "Corrupt .qst file, expected at least 2 headers but only found ${headers.size}.",
            )
            .failure()
    }

    var version: Version? = null
    var online: Boolean? = null

    for (header in headers) {
        if (version != null && header.version != version) {
            return result
                .addProblem(
                    Severity.Error,
                    "This .qst file is corrupt.",
                    "Corrupt .qst file, header version ${header.version} for file ${
                        header.filename
                    } doesn't match the previous header's version ${version}.",
                )
                .failure()
        }

        if (online != null && header.online != online) {
            return result
                .addProblem(
                    Severity.Error,
                    "This .qst file is corrupt.",
                    "Corrupt .qst file, header type ${
                        if (header.online) "\"online\"" else "\"download\""
                    } for file ${header.filename} doesn't match the previous header's type ${
                        if (online) "\"online\"" else "\"download\""
                    }.",
                )
                .failure()
        }

        version = header.version
        online = header.online
    }

    checkNotNull(version)
    checkNotNull(online)

    val parseFilesResult: PwResult<List<QstContainedFile>> = parseFiles(
        cursor,
        version,
        headers.associateBy { it.filename },
    )
    result.addResult(parseFilesResult)

    if (parseFilesResult !is Success) {
        return result.failure()
    }

    return result.success(
        QstContent(
            version,
            online,
            parseFilesResult.value,
        )
    )
}

private class QstHeader(
    val version: Version,
    val online: Boolean,
    val questId: Int,
    val name: String,
    val filename: String,
    val size: Int,
)

private fun parseHeaders(cursor: Cursor): List<QstHeader> {
    val headers = mutableListOf<QstHeader>()

    var prevQuestId: Int? = null
    var prevFilename: String? = null

    // .qst files should have two headers. Some malformed files have more, so we tried to detect at
    // most 4 headers.
    repeat(4) {
        // Detect version and whether it's an online or download quest.
        val version: Version
        val online: Boolean

        val versionA = cursor.uByte().toInt()
        cursor.seek(1)
        val versionB = cursor.uByte().toInt()
        cursor.seek(-3)

        if (versionA == BB_HEADER_SIZE && versionB == ONLINE_QUEST) {
            version = Version.BB
            online = true
        } else if (versionA == DC_GC_PC_HEADER_SIZE && versionB == ONLINE_QUEST) {
            version = Version.PC
            online = true
        } else if (versionB == DC_GC_PC_HEADER_SIZE) {
            val pos = cursor.position
            cursor.seek(35)

            version = if (cursor.byte().toInt() == 0) {
                Version.GC
            } else {
                Version.DC
            }

            cursor.seekStart(pos)

            online = when (versionA) {
                ONLINE_QUEST -> true
                DOWNLOAD_QUEST -> false
                else -> return@repeat
            }
        } else {
            return@repeat
        }

        // Read header.
        val headerSize: Int
        val questId: Int
        val name: String
        val filename: String
        val size: Int

        when (version) {
            Version.DC -> {
                cursor.seek(1) // Skip online/download.
                questId = cursor.uByte().toInt()
                headerSize = cursor.uShort().toInt()
                name = cursor.stringAscii(32, nullTerminated = true, dropRemaining = true)
                cursor.seek(3)
                filename = cursor.stringAscii(16, nullTerminated = true, dropRemaining = true)
                cursor.seek(1)
                size = cursor.int()
            }

            Version.GC -> {
                cursor.seek(1) // Skip online/download.
                questId = cursor.uByte().toInt()
                headerSize = cursor.uShort().toInt()
                name = cursor.stringAscii(32, nullTerminated = true, dropRemaining = true)
                cursor.seek(4)
                filename = cursor.stringAscii(16, nullTerminated = true, dropRemaining = true)
                size = cursor.int()
            }

            Version.PC -> {
                headerSize = cursor.uShort().toInt()
                cursor.seek(1) // Skip online/download.
                questId = cursor.uByte().toInt()
                name = cursor.stringAscii(32, nullTerminated = true, dropRemaining = true)
                cursor.seek(4)
                filename = cursor.stringAscii(16, nullTerminated = true, dropRemaining = true)
                size = cursor.int()
            }

            Version.BB -> {
                headerSize = cursor.uShort().toInt()
                cursor.seek(2) // Skip online/download.
                questId = cursor.uShort().toInt()
                cursor.seek(38)
                filename = cursor.stringAscii(16, nullTerminated = true, dropRemaining = true)
                size = cursor.int()
                name = cursor.stringAscii(24, nullTerminated = true, dropRemaining = true)
            }
        }

        // Use some simple heuristics to figure out whether the file contains more than two headers.
        // Some malformed .qst files have extra headers.
        if (
            prevQuestId != null &&
            prevFilename != null &&
            (questId != prevQuestId || filenameBase(filename) != filenameBase(prevFilename!!))
        ) {
            cursor.seek(-headerSize)
            return@repeat
        }

        prevQuestId = questId
        prevFilename = filename

        headers.add(
            QstHeader(
                version,
                online,
                questId,
                name,
                filename,
                size,
            )
        )
    }

    return headers
}

private class QstFileData(
    val name: String,
    val expectedSize: Int?,
    val cursor: WritableCursor,
    var chunkNos: MutableSet<Int>,
)

private fun parseFiles(
    cursor: Cursor,
    version: Version,
    headers: Map<String, QstHeader>,
): PwResult<List<QstContainedFile>> {
    val result = PwResult.build<List<QstContainedFile>>(logger)

    // Files are interleaved in 1048 or 1056 byte chunks, depending on the version.
    // Each chunk has a 20 or 24 byte header, 1024 byte data segment and a 4 or 8 byte trailer.
    val files = mutableMapOf<String, QstFileData>()

    val chunkSize: Int // Size including padding, header and trailer.
    val trailerSize: Int

    when (version) {
        Version.DC,
        Version.GC,
        Version.PC,
        -> {
            chunkSize = DC_GC_PC_CHUNK_SIZE
            trailerSize = DC_GC_PC_CHUNK_TRAILER_SIZE
        }

        Version.BB -> {
            chunkSize = BB_CHUNK_SIZE
            trailerSize = BB_CHUNK_TRAILER_SIZE
        }
    }

    while (cursor.bytesLeft >= chunkSize) {
        val startPosition = cursor.position

        // Read chunk header.
        var chunkNo: Int

        when (version) {
            Version.DC,
            Version.GC,
            -> {
                cursor.seek(1)
                chunkNo = cursor.uByte().toInt()
                cursor.seek(2)
            }

            Version.PC -> {
                cursor.seek(3)
                chunkNo = cursor.uByte().toInt()
            }

            Version.BB -> {
                cursor.seek(4)
                chunkNo = cursor.int()
            }
        }

        val fileName = cursor.stringAscii(16, nullTerminated = true, dropRemaining = true)

        val file = files.getOrPut(fileName) {
            val header = headers[fileName]
            QstFileData(
                fileName,
                header?.size,
                Buffer.withCapacity(
                    header?.size ?: (10 * CHUNK_BODY_SIZE),
                    Endianness.Little
                ).cursor(),
                mutableSetOf()
            )
        }

        if (chunkNo in file.chunkNos) {
            result.addProblem(
                Severity.Warning,
                "File chunk Int $chunkNo of file $fileName was already encountered, overwriting previous chunk.",
            )
        } else {
            file.chunkNos.add(chunkNo)
        }

        // Read file data.
        var size = cursor.seek(CHUNK_BODY_SIZE).int()
        cursor.seek(-CHUNK_BODY_SIZE - 4)

        if (size > CHUNK_BODY_SIZE) {
            result.addProblem(
                Severity.Warning,
                "Data segment size of $size is larger than expected maximum size, reading just $CHUNK_BODY_SIZE bytes.",
            )
            size = CHUNK_BODY_SIZE
        }

        val data = cursor.take(size)
        val chunkPosition = chunkNo * CHUNK_BODY_SIZE
        file.cursor.size = max(chunkPosition + size, file.cursor.size)
        file.cursor.seekStart(chunkPosition).writeCursor(data)

        // Skip the padding and the trailer.
        cursor.seek(CHUNK_BODY_SIZE - data.size + trailerSize)

        check(cursor.position == startPosition + chunkSize) {
            "Read ${
                cursor.position - startPosition
            } file chunk message bytes instead of expected ${chunkSize}."
        }
    }

    if (cursor.hasBytesLeft()) {
        result.addProblem(Severity.Warning, "${cursor.bytesLeft} Bytes left in file.")
    }

    for (file in files.values) {
        // Clean up file properties.
        file.cursor.seekStart(0)
        file.chunkNos = file.chunkNos.sorted().toMutableSet()

        // Check whether the expected size was correct.
        if (file.expectedSize != null && file.cursor.size != file.expectedSize) {
            result.addProblem(
                Severity.Warning,
                "File ${file.name} has an actual size of ${
                    file.cursor.size
                } instead of the expected size ${file.expectedSize}.",
            )
        }

        // Detect missing file chunks.
        val actualSize = max(file.cursor.size, file.expectedSize ?: 0)
        val expectedChunkCount = ceil(actualSize.toDouble() / CHUNK_BODY_SIZE).toInt()

        for (chunkNo in 0 until expectedChunkCount) {
            if (chunkNo !in file.chunkNos) {
                result.addProblem(
                    Severity.Warning,
                    "File ${file.name} is missing chunk ${chunkNo}.",
                )
            }
        }
    }

    return result.success(
        files.values.map { file ->
            val header = headers[file.name]
            QstContainedFile(
                header?.questId,
                file.name,
                header?.name,
                file.cursor.seekStart(0).buffer(),
            )
        }
    )
}

fun writeQst(qst: QstContent): Buffer {
    val fileHeaderSize: Int
    val chunkSize: Int

    when (qst.version) {
        Version.DC, Version.GC, Version.PC -> {
            fileHeaderSize = DC_GC_PC_HEADER_SIZE
            chunkSize = DC_GC_PC_CHUNK_SIZE
        }
        Version.BB -> {
            fileHeaderSize = BB_HEADER_SIZE
            chunkSize = BB_CHUNK_SIZE
        }
    }

    val totalSize = qst.files.sumOf {
        fileHeaderSize + ceil(it.data.size.toDouble() / CHUNK_BODY_SIZE).toInt() * chunkSize
    }

    val buffer = Buffer.withCapacity(totalSize)
    val cursor = buffer.cursor()

    writeFileHeaders(cursor, qst.files, qst.version, qst.online, fileHeaderSize)
    writeFileChunks(cursor, qst.files, qst.version)

    check(cursor.position == totalSize) {
        "Expected a final file size of $totalSize, but got ${cursor.position}."
    }

    return buffer
}

private fun writeFileHeaders(
    cursor: WritableCursor,
    files: List<QstContainedFile>,
    version: Version,
    online: Boolean,
    headerSize: Int,
) {
    val maxId: Int
    val maxQuestNameLength: Int

    if (version == Version.BB) {
        maxId = 0xffff
        maxQuestNameLength = 23
    } else {
        maxId = 0xff
        maxQuestNameLength = 31
    }

    for (file in files) {
        require(file.id == null || (file.id in 0..maxId)) {
            "Quest ID should be between 0 and $maxId, inclusive."
        }
        require(file.questName == null || file.questName.length <= maxQuestNameLength) {
            "File ${file.filename} has a quest name longer than $maxQuestNameLength characters (${file.questName})."
        }
        require(file.filename.length <= 15) {
            "File ${file.filename} has a filename longer than 15 characters."
        }

        when (version) {
            Version.DC -> {
                cursor.writeUByte((if (online) ONLINE_QUEST else DOWNLOAD_QUEST).toUByte())
                cursor.writeUByte(file.id?.toUByte() ?: 0u)
                cursor.writeUShort(headerSize.toUShort())
                cursor.writeStringAscii(file.questName ?: file.filename, 32)
                cursor.writeByte(0)
                cursor.writeByte(0)
                cursor.writeByte(0)
                cursor.writeStringAscii(file.filename, 16)
                cursor.writeByte(0)
                cursor.writeInt(file.data.size)
            }

            Version.GC -> {
                cursor.writeUByte((if (online) ONLINE_QUEST else DOWNLOAD_QUEST).toUByte())
                cursor.writeUByte(file.id?.toUByte() ?: 0u)
                cursor.writeUShort(headerSize.toUShort())
                cursor.writeStringAscii(file.questName ?: file.filename, 32)
                cursor.writeInt(0)
                cursor.writeStringAscii(file.filename, 16)
                cursor.writeInt(file.data.size)
            }

            Version.PC -> {
                cursor.writeUShort(headerSize.toUShort())
                cursor.writeUByte((if (online) ONLINE_QUEST else DOWNLOAD_QUEST).toUByte())
                cursor.writeUByte(file.id?.toUByte() ?: 0u)
                cursor.writeStringAscii(file.questName ?: file.filename, 32)
                cursor.writeInt(0)
                cursor.writeStringAscii(file.filename, 16)
                cursor.writeInt(file.data.size)
            }

            Version.BB -> {
                cursor.writeUShort(headerSize.toUShort())
                cursor.writeUShort((if (online) ONLINE_QUEST else DOWNLOAD_QUEST).toUShort())
                cursor.writeUShort(file.id?.toUShort() ?: 0u)
                repeat(38) { cursor.writeByte(0) }
                cursor.writeStringAscii(file.filename, 16)
                cursor.writeInt(file.data.size)
                cursor.writeStringAscii(file.questName ?: file.filename, 24)
            }
        }
    }
}

private class FileToChunk(
    var no: Int,
    val data: Cursor,
    val name: String,
)

private fun writeFileChunks(
    cursor: WritableCursor,
    files: List<QstContainedFile>,
    version: Version,
) {
    // Files are interleaved in chunks. Each chunk has a header, fixed-size data segment and a
    // trailer.
    val filesToChunk = files.map { file ->
        FileToChunk(
            no = 0,
            data = file.data.cursor(),
            name = file.filename,
        )
    }
    var done = 0

    while (done < filesToChunk.size) {
        for (fileToChunk in filesToChunk) {
            if (fileToChunk.data.hasBytesLeft()) {
                if (
                    !writeFileChunk(
                        cursor,
                        fileToChunk.data,
                        fileToChunk.no++,
                        fileToChunk.name,
                        version,
                    )
                ) {
                    done++
                }
            }
        }
    }

    for (fileToChunk in filesToChunk) {
        val expectedChunks = ceil(fileToChunk.data.size.toDouble() / CHUNK_BODY_SIZE).toInt()

        check(fileToChunk.no == expectedChunks) {
            """Expected to write $expectedChunks chunks for file "${
                fileToChunk.name
            }" but ${fileToChunk.no} where written."""
        }
    }
}

/**
 * Returns true if there are bytes left to write in [data], false otherwise.
 */
private fun writeFileChunk(
    cursor: WritableCursor,
    data: Cursor,
    chunkNo: Int,
    name: String,
    version: Version,
): Boolean {
    when (version) {
        Version.DC,
        Version.GC,
        -> {
            cursor.writeByte(0)
            cursor.writeUByte(chunkNo.toUByte())
            cursor.writeShort(0)
        }

        Version.PC -> {
            cursor.writeByte(0)
            cursor.writeByte(0)
            cursor.writeByte(0)
            cursor.writeUByte(chunkNo.toUByte())
        }

        Version.BB -> {
            cursor.writeByte(28)
            cursor.writeByte(4)
            cursor.writeByte(19)
            cursor.writeByte(0)
            cursor.writeInt(chunkNo)
        }
    }

    cursor.writeStringAscii(name, 16)

    val size = min(CHUNK_BODY_SIZE, data.bytesLeft)
    cursor.writeCursor(data.take(size))

    // Padding.
    repeat(CHUNK_BODY_SIZE - size) {
        cursor.writeByte(0)
    }

    cursor.writeInt(size)

    if (version == Version.BB) {
        cursor.writeInt(0)
    }

    return data.hasBytesLeft()
}
