package world.phantasmal.web

import mu.DefaultMessageFormatter
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
        time() + DefaultMessageFormatter.formatMessage(level, loggerName, msg)

    override fun formatMessage(
        level: KotlinLoggingLevel,
        loggerName: String,
        t: Throwable?,
        msg: () -> Any?,
    ): String =
        time() + DefaultMessageFormatter.formatMessage(level, loggerName, t, msg)

    override fun formatMessage(
        level: KotlinLoggingLevel,
        loggerName: String,
        marker: Marker?,
        msg: () -> Any?,
    ): String =
        time() + DefaultMessageFormatter.formatMessage(level, loggerName, marker, msg)

    override fun formatMessage(
        level: KotlinLoggingLevel,
        loggerName: String,
        marker: Marker?,
        t: Throwable?,
        msg: () -> Any?,
    ): String =
        time() + DefaultMessageFormatter.formatMessage(level, loggerName, marker, t, msg)

    private fun time(): String {
        val date = Date()
        val h = date.getHours().toString().padStart(2, '0')
        val m = date.getMinutes().toString().padStart(2, '0')
        val s = date.getSeconds().toString().padStart(2, '0')
        val ms = date.getMilliseconds().toString().padStart(3, '0')
        return "$h:$m:$s.$ms "
    }
}
