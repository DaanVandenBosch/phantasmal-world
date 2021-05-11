package world.phantasmal.web.shared.logging

import mu.Formatter
import mu.KotlinLoggingLevel
import mu.Marker
import kotlin.js.Date

class LogFormatter : Formatter {
    override fun formatMessage(
        level: KotlinLoggingLevel,
        loggerName: String,
        msg: () -> Any?,
    ): String =
        "${time()} ${level.str()} $loggerName - ${msg.toStringSafe()}"

    override fun formatMessage(
        level: KotlinLoggingLevel,
        loggerName: String,
        t: Throwable?,
        msg: () -> Any?,
    ): MessageWithThrowable =
        MessageWithThrowable(formatMessage(level, loggerName, msg), t)

    override fun formatMessage(
        level: KotlinLoggingLevel,
        loggerName: String,
        marker: Marker?,
        msg: () -> Any?,
    ): String =
        "${time()} ${level.str()} $loggerName [${marker?.getName()}] - ${msg.toStringSafe()}"

    override fun formatMessage(
        level: KotlinLoggingLevel,
        loggerName: String,
        marker: Marker?,
        t: Throwable?,
        msg: () -> Any?,
    ): MessageWithThrowable =
        MessageWithThrowable(formatMessage(level, loggerName, marker, msg), t)

    @Suppress("NOTHING_TO_INLINE")
    private inline fun (() -> Any?).toStringSafe(): String {
        return try {
            invoke().toString()
        } catch (e: Exception) {
            "Log message invocation failed: $e"
        }
    }

    private fun KotlinLoggingLevel.str(): String =
        name.padEnd(MIN_LEVEL_LEN)

    private fun time(): String {
        val date = Date()
        val h = date.getHours().toString().padStart(2, '0')
        val m = date.getMinutes().toString().padStart(2, '0')
        val s = date.getSeconds().toString().padStart(2, '0')
        val ms = date.getMilliseconds().toString().padStart(3, '0')
        return "$h:$m:$s.$ms"
    }

    companion object {
        private val MIN_LEVEL_LEN: Int =
            KotlinLoggingLevel.values().map { it.name.length }.maxOrNull()!!
    }
}
