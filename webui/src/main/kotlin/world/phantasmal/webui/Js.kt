package world.phantasmal.webui

import kotlinx.browser.window
import org.w3c.files.File
import world.phantasmal.core.filenameExtension

object BrowserFeatures {
    val fileSystemApi: Boolean = window.asDynamic().showOpenFilePicker != null
}

inline fun <T> obj(block: T.() -> Unit): T =
    js("{}").unsafeCast<T>().apply(block)

fun File.extension(): String? =
    filenameExtension(name)
