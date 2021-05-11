package world.phantasmal.web.questEditor.asm.monaco

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import world.phantasmal.web.externals.monacoEditor.*
import world.phantasmal.web.questEditor.asm.AsmAnalyser
import world.phantasmal.webui.obj
import kotlin.js.Promise

class AsmDocumentHighlightProvider(private val analyser: AsmAnalyser) : DocumentHighlightProvider {
    override fun provideDocumentHighlights(
        model: ITextModel,
        position: Position,
        token: CancellationToken
    ): Promise<Array<DocumentHighlight>> =
        GlobalScope.promise {
            val highlights = analyser.getHighlights(position.lineNumber, position.column)

            Array(highlights.size) {
                obj {
                    range = highlights[it].toIRange()
                }
            }
        }
}
