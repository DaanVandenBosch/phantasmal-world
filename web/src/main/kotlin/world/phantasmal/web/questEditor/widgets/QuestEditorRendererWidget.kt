package world.phantasmal.web.questEditor.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.web.externals.Engine

class QuestEditorRendererWidget(
    scope: CoroutineScope,
    createEngine: (HTMLCanvasElement) -> Engine,
) : QuestRendererWidget(scope, createEngine) {
}
