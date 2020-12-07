package world.phantasmal.web.questEditor.asm

class TextRange(
    var startLineNo: Int,
    var startCol: Int,
    var endLineNo: Int,
    var endCol: Int,
)

enum class CompletionItemType {
    Keyword, Opcode
}

class CompletionItem(val label: String, val type: CompletionItemType, val insertText: String)

class SignatureHelp(val signature: Signature, val activeParameter: Int)

class Signature(val label: String, val documentation: String?, val parameters: List<Parameter>)

class Parameter(
    /**
     * Start column of the parameter label within [Signature.label].
     */
    val labelStart: Int,
    /**
     * End column (exclusive) of the parameter label within [Signature.label].
     */
    val labelEnd: Int,
    val documentation: String?,
)

class Hover(
    /**
     * List of markdown strings.
     */
    val contents: List<String>,
)
