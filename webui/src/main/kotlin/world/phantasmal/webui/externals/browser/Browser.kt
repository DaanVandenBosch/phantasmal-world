package world.phantasmal.webui.externals.browser

import org.khronos.webgl.ArrayBuffer
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
