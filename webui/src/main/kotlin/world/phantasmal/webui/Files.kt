package world.phantasmal.webui

import kotlinx.browser.document
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.ArrayBuffer
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList
import org.w3c.files.File
import org.w3c.files.FileReader

fun openFiles(accept: String = "", multiple: Boolean = false, callback: (List<File>) -> Unit) {
    val el = document.createElement("input") as HTMLInputElement
    el.type = "file"
    el.accept = accept
    el.multiple = multiple

    el.onchange = {
        callback(el.files?.asList() ?: emptyList())
    }

    el.click()
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun readFile(file: File): ArrayBuffer = suspendCancellableCoroutine { cont ->
    val reader = FileReader()
    reader.onloadend = {
        if (reader.result is ArrayBuffer) {
            cont.resume(reader.result.unsafeCast<ArrayBuffer>()) {}
        } else {
            cont.cancel(Exception(reader.error.message.unsafeCast<String>()))
        }
    }
    reader.readAsArrayBuffer(file)
}
