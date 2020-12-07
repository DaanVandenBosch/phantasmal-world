package world.phantasmal.web.questEditor.asm

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import world.phantasmal.web.externals.monacoEditor.*
import world.phantasmal.webui.obj
import kotlin.js.Promise

object AsmCompletionItemProvider : CompletionItemProvider {
    override fun provideCompletionItems(
        model: ITextModel,
        position: Position,
        context: CompletionContext,
        token: CancellationToken,
    ): Promise<CompletionList> =
        GlobalScope.promise {
            val completions = AsmAnalyser.getCompletions(
                position.lineNumber,
                position.column,
            )

            obj {
                suggestions = Array(completions.size) { i ->
                    val completion = completions[i]

                    obj {
                        label = obj { name = completion.label }
                        kind = when (completion.type) {
                            CompletionItemType.Keyword -> CompletionItemKind.Keyword
                            CompletionItemType.Opcode -> CompletionItemKind.Function
                        }
                        insertText = completion.insertText
                    }
                }
                incomplete = false
            }
        }
}
