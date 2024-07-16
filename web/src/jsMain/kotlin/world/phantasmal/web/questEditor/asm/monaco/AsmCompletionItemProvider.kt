package world.phantasmal.web.questEditor.asm.monaco

import kotlinx.coroutines.promise
import world.phantasmal.web.externals.monacoEditor.*
import world.phantasmal.web.questEditor.asm.AsmAnalyser
import world.phantasmal.web.shared.messages.CompletionItemType
import world.phantasmal.webui.obj
import kotlin.js.Promise

class AsmCompletionItemProvider(private val analyser: AsmAnalyser) :
    MonacoProvider(), CompletionItemProvider {
    override fun provideCompletionItems(
        model: ITextModel,
        position: Position,
        context: CompletionContext,
        token: CancellationToken,
    ): Promise<CompletionList> =
        scope.promise {
            val completions = analyser.getCompletions(
                position.lineNumber,
                position.column,
            )

            obj {
                suggestions = Array(completions.size) { i ->
                    val completion = completions[i]

                    obj {
                        label = obj { label = completion.label }
                        kind = when (completion.type) {
                            CompletionItemType.Keyword -> CompletionItemKind.Keyword
                            CompletionItemType.Opcode -> CompletionItemKind.Function
                        }
                        insertText = completion.insertText
                        completion.detail?.let { detail = it }
                        completion.documentation?.let { documentation = it }
                    }
                }
                incomplete = false
            }
        }
}
