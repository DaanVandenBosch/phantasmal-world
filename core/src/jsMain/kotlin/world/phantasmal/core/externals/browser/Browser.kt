@file:Suppress("unused")

package world.phantasmal.core.externals.browser

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.BufferDataSource
import org.w3c.dom.Window
import org.w3c.files.Blob
import org.w3c.files.File
import kotlin.js.Promise

fun Blob.arrayBuffer(): Promise<ArrayBuffer> =
    asDynamic().arrayBuffer().unsafeCast<Promise<ArrayBuffer>>()

open external class FileSystemHandle {
    val kind: String /* "file" | "directory" */
    val name: String
}

external class FileSystemFileHandle : FileSystemHandle {
    fun getFile(): Promise<File>

    fun createWritable(): Promise<FileSystemWritableFileStream>
}

open external class WritableStream {
    val locked: Boolean

    fun abort(reason: Any): Promise<Any>

    fun close(): Promise<Unit>
}

external interface FileSystemWritableFileStreamData {
    var type: String /* "write" | "seek" | "truncate" */
    var data: dynamic /* BufferDataSource | Blob | String */
    var position: Int
    var size: Int
}

external class FileSystemWritableFileStream : WritableStream {
    fun write(data: BufferDataSource): Promise<Unit>
    fun write(data: FileSystemWritableFileStreamData): Promise<Unit>

    fun seek(position: Int): Promise<Unit>
}

external interface ShowOpenFilePickerOptionsType {
    var description: String
    var accept: dynamic
}

external interface ShowOpenFilePickerOptions {
    var multiple: Boolean
    var excludeAcceptAllOption: Boolean
    var types: Array<ShowOpenFilePickerOptionsType>
}

fun Window.showOpenFilePicker(
    options: ShowOpenFilePickerOptions,
): Promise<Array<FileSystemFileHandle>> =
    asDynamic().showOpenFilePicker(options).unsafeCast<Promise<Array<FileSystemFileHandle>>>()
