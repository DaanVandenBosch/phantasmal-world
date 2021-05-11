package world.phantasmal.core

import mu.KLogger

sealed class PwResult<out T>(val problems: List<Problem>) {
    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

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

open class Problem(
    val severity: Severity,
    /**
     * Readable message meant for users.
     */
    val uiMessage: String,
    /**
     * Message meant for developers.
     */
    val message: String? = null,
    val cause: Throwable? = null,
)

enum class Severity {
    Trace,
    Debug,
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
        problem: Problem,
    ): PwResultBuilder<T> {
        when (problem.severity) {
            Severity.Trace -> logger.trace(problem.cause) { problem.message ?: problem.uiMessage }
            Severity.Debug -> logger.debug(problem.cause) { problem.message ?: problem.uiMessage }
            Severity.Info -> logger.info(problem.cause) { problem.message ?: problem.uiMessage }
            Severity.Warning -> logger.warn(problem.cause) { problem.message ?: problem.uiMessage }
            Severity.Error -> logger.error(problem.cause) { problem.message ?: problem.uiMessage }
        }

        problems.add(problem)
        return this
    }

    /**
     * Add a problem to the problems list and log it with [logger].
     */
    fun addProblem(
        severity: Severity,
        uiMessage: String,
        message: String? = null,
        cause: Throwable? = null,
    ): PwResultBuilder<T> =
        addProblem(Problem(severity, uiMessage, message, cause))

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
