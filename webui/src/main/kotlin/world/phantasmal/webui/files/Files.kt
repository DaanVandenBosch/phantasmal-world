package world.phantasmal.webui.files

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.await
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.ArrayBuffer
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList
import org.w3c.dom.events.Event
import org.w3c.files.File
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.externals.browser.*
import world.phantasmal.core.filenameBase
import world.phantasmal.core.filenameExtension
import world.phantasmal.webui.BrowserFeatures
import world.phantasmal.webui.dom.disposableListener
import world.phantasmal.webui.obj
import kotlin.coroutines.resume

sealed class FileHandle {
    abstract val name: String

    /**
     * Returns the filename without extension if there is one.
     */
    fun basename(): String? = filenameBase(name)

    /**
     * Returns the filename extension if there is one.
     */
    fun extension(): String? = filenameExtension(name)

    suspend fun arrayBuffer(): ArrayBuffer =
        getFile().arrayBuffer().await()

    protected abstract suspend fun getFile(): File

    /**
     * File system access API file handle.
     */
    class Fsaa(private val handle: FileSystemFileHandle) : FileHandle() {
        override val name: String = handle.name

        suspend fun writableStream(): FileSystemWritableFileStream =
            handle.createWritable().await()

        override suspend fun getFile(): File =
            handle.getFile().await()
    }

    class Simple(private val file: File) : FileHandle() {
        override val name: String = file.name

        override suspend fun getFile(): File = file
    }
}

class FileType(
    val description: String,
    /**
     * Map of MIME types to file extensions
     */
    val accept: Map<String, Set<String>>,
)

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun showFilePicker(types: List<FileType>, multiple: Boolean = false): List<FileHandle>? =
    if (BrowserFeatures.fileSystemApi) {
        try {
            val fileHandles = window.showOpenFilePicker(obj {
                this.multiple = multiple
                this.types = types.map {
                    obj<ShowOpenFilePickerOptionsType> {
                        description = it.description
                        accept = obj {
                            for ((mimeType, extensions) in it.accept) {
                                this[mimeType] = extensions.toTypedArray()
                            }
                        }
                    }
                }.toTypedArray()
            }).await()

            fileHandles.map(FileHandle::Fsaa)
        } catch (e: Throwable) {
            // Ensure we return null when the user cancels.
            if (e.asDynamic().name == "AbortError") {
                null
            } else {
                throw e
            }
        }
    } else {
        suspendCancellableCoroutine { cont ->
            val el = document.createElement("input") as HTMLInputElement
            el.type = "file"
            el.accept = types.flatMap { it.accept.values.flatten() }.joinToString()
            el.multiple = multiple

            el.onchange = {
                cont.resume(el.files!!.asList().map(FileHandle::Simple))
            }

            // Ensure we return null when the user cancels.
            @Suppress("JoinDeclarationAndAssignment")
            lateinit var focusListener: Disposable

            focusListener = window.disposableListener<Event>("focus", {
                focusListener.dispose()

                window.setTimeout({
                    if (cont.isActive) {
                        cont.resume(null)
                    }
                }, 500)
            })

            el.click()
        }
    }
