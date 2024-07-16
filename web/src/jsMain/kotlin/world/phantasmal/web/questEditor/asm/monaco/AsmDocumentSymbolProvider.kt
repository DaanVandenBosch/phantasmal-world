package world.phantasmal.web.questEditor.asm.monaco

import kotlinx.coroutines.promise
import world.phantasmal.web.externals.monacoEditor.*
import world.phantasmal.web.questEditor.asm.AsmAnalyser
import world.phantasmal.webui.obj
import kotlin.js.Promise

// Sometimes produces stack overflows in monaco perf monitoring code due to a monaco bug. Things
// still work as they should, though.
// Chrome error: "Uncaught (in promise) RangeError: Maximum call stack size exceeded"
// https://github.com/microsoft/monaco-editor/issues/2586
// TODO: See whether monaco perf monitoring bug will get fixed or not.
class AsmDocumentSymbolProvider(private val asmAnalyser: AsmAnalyser) :
    MonacoProvider(), DocumentSymbolProvider {
    override val displayName: String? = null

    override fun provideDocumentSymbols(
        model: ITextModel,
        token: CancellationToken
    ): Promise<Array<DocumentSymbol>> =
        scope.promise {
            val labels = asmAnalyser.getLabels()

            Array(labels.size) { index ->
                val label = labels[index]

                obj {
                    name = label.name.toString()
                    detail = ""
                    kind = SymbolKind.Function
                    tags = emptyArray()
                    range = label.range.toIRange()
                    selectionRange = range
                }
            }
        }
}
