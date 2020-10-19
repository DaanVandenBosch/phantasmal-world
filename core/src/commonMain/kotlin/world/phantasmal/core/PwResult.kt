package world.phantasmal.core

import mu.KLogger

sealed class PwResult<out T>(val problems: List<Problem>) {
    fun unwrap(): T = when (this) {
        is Success -> value
        is Failure -> error(problems.joinToString("\n") { "[${it.severity}] ${it.uiMessage}" })
    }

    companion object {
        fun <T> build(logger: KLogger): PwResultBuilder<T> =
            PwResultBuilder(logger)
    }
}

class Success<T>(val value: T, problems: List<Problem> = emptyList()) : PwResult<T>(problems)

class Failure(problems: List<Problem>) : PwResult<Nothing>(problems)

class Problem(
    val severity: Severity,
    /**
     * Readable message meant for users.
     */
    val uiMessage: String,
)

enum class Severity {
    Info,
    Warning,
    Error,
}

/**
 * Useful for building up a [PwResult] and logging problems at the same time.
 */
class PwResultBuilder<T>(private val logger: KLogger) {
    private val problems: MutableList<Problem> = mutableListOf()

    /**
     * Add a problem to the problems list and log it with [logger].
     */
    fun addProblem(
        severity: Severity,
        uiMessage: String,
        message: String? = null,
        cause: Throwable? = null,
    ): PwResultBuilder<T> {
        when (severity) {
            Severity.Info -> logger.info(cause) { message ?: uiMessage }
            Severity.Warning -> logger.warn(cause) { message ?: uiMessage }
            Severity.Error -> logger.error(cause) { message ?: uiMessage }
        }

        problems.add(Problem(severity, uiMessage))
        return this
    }

    /**
     * Add the given result's problems.
     */
    fun addResult(result: PwResult<*>): PwResultBuilder<T> {
        problems.addAll(result.problems)
        return this
    }

    fun success(value: T): Success<T> =
        Success(value, problems)

    fun failure(): Failure =
        Failure(problems)
}
