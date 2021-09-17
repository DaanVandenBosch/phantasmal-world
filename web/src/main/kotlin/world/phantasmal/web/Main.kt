package world.phantasmal.web

import kotlinx.browser.document
import kotlinx.browser.window
import world.phantasmal.psolib.buffer.Buffer

fun main() {
    if (document.body != null) {
        init()
    } else {
        window.addEventListener("DOMContentLoaded", { init() })
    }
}

private fun init() {
    console.log(Buffer.fromByteArray(byteArrayOf()).size)
}
