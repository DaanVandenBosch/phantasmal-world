package world.phantasmal.web.questEditor.widgets

import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.core.disposable.Scope
import world.phantasmal.web.externals.Engine

class QuestEditorRendererWidget(
    scope: Scope,
    createEngine: (HTMLCanvasElement) -> Engine,
) : QuestRendererWidget(scope, createEngine) {
}
