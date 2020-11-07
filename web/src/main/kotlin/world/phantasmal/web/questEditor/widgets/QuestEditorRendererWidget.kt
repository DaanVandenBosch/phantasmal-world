package world.phantasmal.web.questEditor.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.web.questEditor.rendering.QuestRenderer

class QuestEditorRendererWidget(
    scope: CoroutineScope,
    canvas: HTMLCanvasElement,
    renderer: QuestRenderer,
) : QuestRendererWidget(scope, canvas, renderer)
