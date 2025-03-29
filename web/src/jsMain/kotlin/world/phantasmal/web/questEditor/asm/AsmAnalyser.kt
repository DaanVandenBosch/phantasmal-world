package world.phantasmal.web.questEditor.asm

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import org.w3c.dom.Worker
import world.phantasmal.cell.Cell
import world.phantasmal.cell.MutableCell
import world.phantasmal.cell.list.ListCell
import world.phantasmal.cell.list.mutableListCell
import world.phantasmal.cell.mutableCell
import world.phantasmal.web.shared.JSON_FORMAT
import world.phantasmal.web.shared.messages.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

private val logger = KotlinLogging.logger {}

class AsmAnalyser {
    private var inlineStackArgs: Boolean = true
    private var _mapDesignations: MutableCell<Map<Int, Set<Int>>> = mutableCell(emptyMap())
    private val _problems = mutableListCell<AssemblyProblem>()

    private val worker = Worker("/assembly-worker.js")
    private var nextRequestId = atomic(0)

    /**
     * Maps request IDs to continuations.
     */
    private val inFlightRequests = mutableMapOf<Int, CancellableContinuation<*>>()

    val mapDesignations: Cell<Map<Int, Set<Int>>> = _mapDesignations
    val problems: ListCell<AssemblyProblem> = _problems

    init {
        worker.onmessage = { e ->
            val json = e.data as String
            receiveMessage(JSON_FORMAT.decodeFromString(json))
        }
    }

    fun setAsm(asm: List<String>, inlineStackArgs: Boolean) {
        this.inlineStackArgs = inlineStackArgs
        _problems.clear()
        sendMessage(ClientNotification.SetAsm(asm, inlineStackArgs))
    }

    fun updateAsm(changes: List<AsmChange>) {
        sendMessage(ClientNotification.UpdateAsm(changes))
    }

    suspend fun getCompletions(lineNo: Int, col: Int): List<CompletionItem> =
        sendRequest { id -> Request.GetCompletions(id, lineNo, col) }

    suspend fun getSignatureHelp(lineNo: Int, col: Int): SignatureHelp? =
        sendRequest { id -> Request.GetSignatureHelp(id, lineNo, col) }

    suspend fun getHover(lineNo: Int, col: Int): Hover? =
        sendRequest { id -> Request.GetHover(id, lineNo, col) }

    suspend fun getDefinition(lineNo: Int, col: Int): List<AsmRange> =
        sendRequest { id -> Request.GetDefinition(id, lineNo, col) }

    suspend fun getLabels(): List<Label> =
        sendRequest { id -> Request.GetLabels(id) }

    suspend fun getHighlights(lineNo: Int, col: Int): List<AsmRange> =
        sendRequest { id -> Request.GetHighlights(id, lineNo, col) }

    private suspend fun <T> sendRequest(createRequest: (id: Int) -> Request): T {
        val id = nextRequestId.getAndIncrement()

        try {
            return withTimeout(5_000) {
                suspendCancellableCoroutine { cont ->
                    // Store continuation and resume it when we receive a response.
                    inFlightRequests[id] = cont
                    sendMessage(createRequest(id))
                }
            }
        } catch (e: TimeoutCancellationException) {
            inFlightRequests.remove(id)
            throw e
        }
    }

    private fun sendMessage(message: ClientMessage) {
        worker.postMessage(JSON_FORMAT.encodeToString(message))
    }

    private fun receiveMessage(message: ServerMessage) =
        when (message) {
            is ServerNotification.MapDesignations -> {
                _mapDesignations.value = message.mapDesignations
            }

            is ServerNotification.Problems -> {
                _problems.value = message.problems
            }

            is Response<*> -> {
                val continuation = inFlightRequests.remove(message.id)

                if (continuation == null) {
                    logger.warn {
                        "No continuation for ${message::class.simpleName} ${message.id}, possibly due to timeout."
                    }
                } else {
                    continuation.unsafeCast<Continuation<Any>>()
                        .resume(message.result.unsafeCast<Any>())
                }
            }
        }
}
