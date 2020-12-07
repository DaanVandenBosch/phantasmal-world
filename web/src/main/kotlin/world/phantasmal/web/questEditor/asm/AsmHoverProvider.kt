package world.phantasmal.web.questEditor.asm

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import world.phantasmal.web.externals.monacoEditor.CancellationToken
import world.phantasmal.web.externals.monacoEditor.HoverProvider
import world.phantasmal.web.externals.monacoEditor.ITextModel
import world.phantasmal.web.externals.monacoEditor.Position
import world.phantasmal.webui.obj
import kotlin.js.Promise
import world.phantasmal.web.externals.monacoEditor.Hover as MonacoHover

object AsmHoverProvider : HoverProvider {
    override fun provideHover(
        model: ITextModel,
        position: Position,
        token: CancellationToken,
    ): Promise<MonacoHover?> =
        GlobalScope.promise {
            AsmAnalyser.getHover(position.lineNumber, position.column)?.let { hover ->
                obj<MonacoHover> {
                    contents = Array(hover.contents.size) { i ->
                        val content = hover.contents[i]

                        obj {
                            value = content
                        }
                    }
                }
            }
        }
}
