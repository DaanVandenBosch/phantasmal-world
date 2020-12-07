package world.phantasmal.web.questEditor.asm

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import world.phantasmal.web.externals.monacoEditor.*
import world.phantasmal.webui.obj
import kotlin.js.Promise

object AsmDefinitionProvider : DefinitionProvider {
    override fun provideDefinition(
        model: ITextModel,
        position: Position,
        token: CancellationToken,
    ): Promise<Array<LocationLink>?> =
        GlobalScope.promise {
            val defs = AsmAnalyser.getDefinition(position.lineNumber, position.column)

            Array(defs.size) {
                val def = defs[it]

                obj {
                    uri = model.uri
                    range = obj {
                        startLineNumber = def.startLineNo
                        startColumn = def.startCol
                        endLineNumber = def.endLineNo
                        endColumn = def.endCol
                    }
                }
            }
        }
}
