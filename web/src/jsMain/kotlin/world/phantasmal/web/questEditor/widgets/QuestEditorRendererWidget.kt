package world.phantasmal.web.questEditor.widgets

import world.phantasmal.cell.Cell
import world.phantasmal.web.externals.three.Vector3
import world.phantasmal.web.questEditor.rendering.QuestRenderer

class QuestEditorRendererWidget(
    renderer: QuestRenderer,
    mouseWorldPosition: Cell<Vector3?>,
) : QuestRendererWidget(renderer, mouseWorldPosition)
