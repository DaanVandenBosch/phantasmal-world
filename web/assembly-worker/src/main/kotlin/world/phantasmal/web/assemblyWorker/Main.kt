package world.phantasmal.web.assemblyWorker

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import mu.KotlinLoggingConfiguration
import mu.KotlinLoggingLevel
import world.phantasmal.web.shared.JSON_FORMAT
import world.phantasmal.web.shared.externals.self
import world.phantasmal.web.shared.logging.LogAppender
import world.phantasmal.web.shared.logging.LogFormatter

fun main() {
    KotlinLoggingConfiguration.FORMATTER = LogFormatter()
    KotlinLoggingConfiguration.APPENDER = LogAppender()

    if (self.location.hostname == "localhost") {
        KotlinLoggingConfiguration.LOG_LEVEL = KotlinLoggingLevel.TRACE
    }

    val asmServer = AsmServer(
        AsmAnalyser(),
        sendMessage = { message ->
            self.postMessage(JSON_FORMAT.encodeToString(message))
        }
    )

    self.onmessage = { e ->
        val json = e.data as String
        asmServer.receiveMessage(JSON_FORMAT.decodeFromString(json))
    }
}
