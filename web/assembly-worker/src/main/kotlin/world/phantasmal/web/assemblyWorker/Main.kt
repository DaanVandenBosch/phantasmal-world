package world.phantasmal.web.assemblyWorker

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.w3c.dom.DedicatedWorkerGlobalScope
import world.phantasmal.web.shared.JSON_FORMAT

external val self: DedicatedWorkerGlobalScope

fun main() {
    val asmWorker = AssemblyWorker(
        sendMessage = { message ->
            self.postMessage(JSON_FORMAT.encodeToString(message))
        }
    )

    self.onmessage = { e ->
        val json = e.data as String
        asmWorker.receiveMessage(JSON_FORMAT.decodeFromString(json))
    }
}
