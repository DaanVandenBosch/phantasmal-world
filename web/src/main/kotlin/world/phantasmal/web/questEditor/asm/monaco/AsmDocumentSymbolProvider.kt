package world.phantasmal.web.questEditor.asm.monaco

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import world.phantasmal.web.externals.monacoEditor.CancellationToken
import world.phantasmal.web.externals.monacoEditor.DocumentSymbol
import world.phantasmal.web.externals.monacoEditor.DocumentSymbolProvider
import world.phantasmal.web.externals.monacoEditor.ITextModel
import world.phantasmal.web.questEditor.asm.AsmAnalyser
import world.phantasmal.webui.obj
import kotlin.js.Promise

class AsmDocumentSymbolProvider(private val asmAnalyser: AsmAnalyser) : DocumentSymbolProvider {
    override val displayName: String? = null

    override fun provideDocumentSymbols(
        model: ITextModel,
        token: CancellationToken
    ): Promise<Array<DocumentSymbol>> =
        GlobalScope.promise {
            val labels = asmAnalyser.getLabels()

            Array(labels.size) { index ->
                val label = labels[index]

                obj {
                    name = label.name.toString()
                    label.range?.let { range = it.toIRange() }
                }
            }
        }
}
