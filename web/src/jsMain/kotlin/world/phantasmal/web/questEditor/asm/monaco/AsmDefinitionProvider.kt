package world.phantasmal.web.questEditor.asm.monaco

import kotlinx.coroutines.promise
import world.phantasmal.web.externals.monacoEditor.*
import world.phantasmal.web.questEditor.asm.AsmAnalyser
import world.phantasmal.webui.obj
import kotlin.js.Promise

class AsmDefinitionProvider(private val analyser: AsmAnalyser) :
    MonacoProvider(), DefinitionProvider {
    override fun provideDefinition(
        model: ITextModel,
        position: Position,
        token: CancellationToken,
    ): Promise<Array<LocationLink>?> =
        scope.promise {
            val defs = analyser.getDefinition(position.lineNumber, position.column)

            Array(defs.size) {
                obj {
                    uri = model.uri
                    range = defs[it].toIRange()
                }
            }
        }
}
