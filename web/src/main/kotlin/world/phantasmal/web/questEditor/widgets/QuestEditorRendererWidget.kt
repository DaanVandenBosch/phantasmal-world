package world.phantasmal.web.questEditor.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.web.questEditor.rendering.QuestRenderer

class QuestEditorRendererWidget(
    scope: CoroutineScope,
    renderer: QuestRenderer,
) : QuestRendererWidget(scope, renderer)
