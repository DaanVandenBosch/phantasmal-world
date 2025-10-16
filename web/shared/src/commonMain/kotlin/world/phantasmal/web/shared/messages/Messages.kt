package world.phantasmal.web.shared.messages

import kotlinx.serialization.Serializable
import world.phantasmal.core.Severity

/*
 * The protocol between the AsmAnalyser and the assembly web worker is loosely based on the language
 * server protocol. With the idea that at some point we might implement a full LSP server.
 *
 * There are 3 kinds of messages:
 * - Request: sent by the client expecting a response.
 * - Response: sent by server in response to a request.
 * - Notifications: sent by either the client or server. No response is required.
 */

@Serializable
sealed class ClientMessage

@Serializable
sealed class ClientNotification : ClientMessage() {
    @Serializable
    class SetAsm(
        val asm: List<String>,
        val inlineStackArgs: Boolean,
    ) : ClientNotification()

    @Serializable
    class UpdateAsm(
        val changes: List<AsmChange>,
    ) : ClientNotification()
}

@Serializable
sealed class Request : ClientMessage() {
    /**
     * Will be echoed in the response so the client can match responses to requests.
     */
    abstract val id: Int

    @Serializable
    class GetCompletions(override val id: Int, val lineNo: Int, val col: Int) : Request()

    @Serializable
    class GetSignatureHelp(override val id: Int, val lineNo: Int, val col: Int) : Request()

    @Serializable
    class GetHover(override val id: Int, val lineNo: Int, val col: Int) : Request()

    @Serializable
    class GetDefinition(override val id: Int, val lineNo: Int, val col: Int) : Request()

    @Serializable
    class GetLabels(override val id: Int) : Request()

    @Serializable
    class GetHighlights(override val id: Int, val lineNo: Int, val col: Int) : Request()
}

@Serializable
sealed class ServerMessage

@Serializable
sealed class ServerNotification : ServerMessage() {
    @Serializable
    class MapDesignations(
        val mapDesignations: Map<Int, Set<Int>>,
    ) : ServerNotification()

    @Serializable
    class Problems(
        val problems: List<AssemblyProblem>,
    ) : ServerNotification()
}

@Serializable
sealed class Response<T> : ServerMessage() {
    /**
     * ID of the related request.
     */
    abstract val id: Int

    abstract val result: T

    @Serializable
    class GetCompletions(
        override val id: Int,
        override val result: List<CompletionItem>,
    ) : Response<List<CompletionItem>>()

    @Serializable
    class GetSignatureHelp(
        override val id: Int,
        override val result: SignatureHelp?,
    ) : Response<SignatureHelp?>()

    @Serializable
    class GetHover(
        override val id: Int,
        override val result: Hover?,
    ) : Response<Hover?>()

    @Serializable
    class GetDefinition(
        override val id: Int,
        override val result: List<AsmRange>,
    ) : Response<List<AsmRange>>()

    @Serializable
    class GetLabels(
        override val id: Int,
        override val result: List<Label>,
    ) : Response<List<Label>>()

    @Serializable
    class GetHighlights(
        override val id: Int,
        override val result: List<AsmRange>,
    ) : Response<List<AsmRange>>()
}

@Serializable
data class AsmRange(
    /**
     * Starting line of the range, inclusive.
     */
    val startLineNo: Int,
    /**
     * Starting column of the range, inclusive.
     */
    val startCol: Int,
    /**
     * Ending line of the range, exclusive.
     */
    val endLineNo: Int,
    /**
     * Ending column of the range, exclusive.
     */
    val endCol: Int,
)

enum class CompletionItemType {
    Keyword, Opcode
}

@Serializable
class CompletionItem(
    val label: String,
    val type: CompletionItemType,
    val detail: String?,
    val documentation: String?,
    val insertText: String,
)

@Serializable
class SignatureHelp(val signature: Signature, val activeParameter: Int)

@Serializable
class Signature(val label: String, val documentation: String?, val parameters: List<Parameter>)

@Serializable
class Parameter(
    /**
     * Start index of the parameter label within [Signature.label].
     */
    val labelStart: Int,
    /**
     * End index (exclusive) of the parameter label within [Signature.label].
     */
    val labelEnd: Int,
    val documentation: String?,
)

@Serializable
class Hover(
    /**
     * List of markdown strings.
     */
    val contents: List<String>,
)

@Serializable
class AsmChange(
    val range: AsmRange,
    val newAsm: String,
)

@Serializable
class AssemblyProblem(
    val severity: Severity,
    val message: String,
    val lineNo: Int,
    val col: Int,
    val len: Int,
)

@Serializable
class Label(
    val name: Int,
    val range: AsmRange,
)
