package world.phantasmal.webui.files

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.ArrayBuffer
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList
import org.w3c.dom.events.Event
import org.w3c.files.File
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.filenameBase
import world.phantasmal.core.filenameExtension
import world.phantasmal.webui.BrowserFeatures
import world.phantasmal.webui.dom.disposableListener
import world.phantasmal.webui.externals.browser.FileSystemFileHandle
import world.phantasmal.webui.externals.browser.ShowOpenFilePickerOptionsType
import world.phantasmal.webui.externals.browser.arrayBuffer
import world.phantasmal.webui.externals.browser.showOpenFilePicker
import world.phantasmal.webui.obj
import kotlin.js.Promise

@OptIn(ExperimentalCoroutinesApi::class)
class FileHandle private constructor(
    private val handle: FileSystemFileHandle?,
    private val file: File?,
) {
    constructor(file: File) : this(null, file)
    constructor(handle: FileSystemFileHandle) : this(handle, null)

    val name: String = handle?.name ?: file!!.name

    init {
        require((handle == null) xor (file == null))
    }

    /**
     * Returns the filename without extension if there is one.
     */
    fun basename(): String? = filenameBase(name)

    /**
     * Returns the filename extension if there is one.
     */
    fun extension(): String? = filenameExtension(name)

    suspend fun arrayBuffer(): ArrayBuffer = suspendCancellableCoroutine { cont ->
        getFile()
            .then { it.arrayBuffer() }
            .then({ cont.resume(it) {} }, cont::cancel)
    }

    private fun getFile(): Promise<File> =
        handle?.getFile() ?: Promise.resolve(file!!)
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
    suspendCancellableCoroutine { cont ->
        if (BrowserFeatures.fileSystemApi) {
            window.showOpenFilePicker(obj {
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
            }).then({ cont.resume(it.map(::FileHandle)) {} }, { cont.resume(null) {} })
        } else {
            val el = document.createElement("input") as HTMLInputElement
            el.type = "file"
            el.accept = types.flatMap { it.accept.values.flatten() }.joinToString()
            el.multiple = multiple

            el.onchange = {
                cont.resume(el.files?.asList()?.map(::FileHandle) ?: emptyList()) {}
            }

            @Suppress("JoinDeclarationAndAssignment")
            lateinit var focusListener: Disposable

            focusListener = window.disposableListener<Event>("focus", {
                focusListener.dispose()

                window.setTimeout({
                    if (cont.isActive) {
                        cont.resume(null) {}
                    }
                }, 500)
            })

            el.click()
        }
    }
