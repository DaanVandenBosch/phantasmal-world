package world.phantasmal.web.core.loading

import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import world.phantasmal.core.externals.browser.FileSystemDirectoryHandle
import world.phantasmal.core.externals.browser.arrayBuffer
import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.psolib.cursor.cursor
import world.phantasmal.psolib.fileFormats.GslIndexEntry
import world.phantasmal.psolib.fileFormats.parseGslIndex
import org.w3c.files.File

/**
 * Provides lazy, on-demand access to files inside a GSL archive (e.g. data.gsl).
 *
 * Only the header/index is parsed upfront. Individual file contents are read via [File.slice] so
 * the entire 72 MB archive is never loaded into memory at once.
 */
class GslArchive private constructor(
    private val file: File,
    private val index: Map<String, GslIndexEntry>,
) {
    /**
     * Read a single file from the archive by name. Returns null if the file is not in the archive.
     */
    suspend fun readFile(name: String): ArrayBuffer? {
        val entry = index[name] ?: return null
        return file.slice(entry.offset, entry.offset + entry.size)
            .arrayBuffer().await()
    }

    companion object {
        /**
         * Open a GSL archive from a directory handle. Returns null if the file doesn't exist.
         *
         * Only reads the header (~73 KB for 1524 entries) to build the index.
         */
        suspend fun open(
            dirHandle: FileSystemDirectoryHandle,
            fileName: String = "data.gsl",
        ): GslArchive? {
            val file = try {
                dirHandle.getFileHandle(fileName).await().getFile().await()
            } catch (_: Throwable) {
                return null
            }

            // Read enough for the header. 1524 entries × 0x30 = ~73 KB. Read 128 KB to be safe.
            val fileSize = file.size.toLong()
            val headerSize = minOf(fileSize, 128L * 1024).toInt()
            val headerBuf = file.slice(0, headerSize).arrayBuffer().await()

            val buffer = Buffer.fromArrayBuffer(headerBuf, Endianness.Little)
            val entries = parseGslIndex(buffer.cursor(), fileSize)
            val index = entries.associateBy { it.name }

            return GslArchive(file, index)
        }
    }
}
