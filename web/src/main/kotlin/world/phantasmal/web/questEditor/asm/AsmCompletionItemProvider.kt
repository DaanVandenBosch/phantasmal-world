package world.phantasmal.web.questEditor.asm

import world.phantasmal.lib.asm.OPCODES
import world.phantasmal.lib.asm.OPCODES_F8
import world.phantasmal.lib.asm.OPCODES_F9
import world.phantasmal.web.externals.monacoEditor.*
import world.phantasmal.webui.obj

object AsmCompletionItemProvider : CompletionItemProvider {
    override fun provideCompletionItems(
        model: ITextModel,
        position: Position,
        context: CompletionContext,
        token: CancellationToken,
    ): CompletionList {
        val text = model.getValueInRange(obj {
            startLineNumber = position.lineNumber
            endLineNumber = position.lineNumber
            startColumn = 1
            endColumn = position.column
        })

        val suggestions = when {
            KEYWORD_REGEX.matches(text) -> KEYWORD_SUGGESTIONS
            INSTRUCTION_REGEX.matches(text) -> INSTRUCTION_SUGGESTIONS
            else -> emptyArray()
        }

        return obj {
            this.suggestions = suggestions
            incomplete = false
        }
    }

    private val KEYWORD_REGEX = Regex("""^\s*\.[a-z]+${'$'}""")
    private val KEYWORD_SUGGESTIONS: Array<CompletionItem> =
        arrayOf(
            obj {
                label = obj { name = ".code" }
                kind = CompletionItemKind.Keyword
                insertText = "code"
            },
            obj {
                label = obj { name = ".data" }
                kind = CompletionItemKind.Keyword
                insertText = "data"
            },
            obj {
                label = obj { name = ".string" }
                kind = CompletionItemKind.Keyword
                insertText = "string"
            },
        )

    private val INSTRUCTION_REGEX = Regex("""^\s*([a-z][a-z0-9_=<>!]*)?${'$'}""")
    private val INSTRUCTION_SUGGESTIONS: Array<CompletionItem> =
        (OPCODES + OPCODES_F8 + OPCODES_F9)
            .filterNotNull()
            .map { opcode ->
                obj<CompletionItem> {
                    label = obj {
                        name = opcode.mnemonic
                        // TODO: Add signature?
                    }
                    kind = CompletionItemKind.Function
                    insertText = opcode.mnemonic
                }
            }
            .toTypedArray()
}
