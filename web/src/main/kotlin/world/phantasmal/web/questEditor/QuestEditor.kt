package world.phantasmal.web.questEditor

import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.core.disposable.Scope
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.externals.Engine
import world.phantasmal.web.questEditor.controllers.QuestEditorToolbarController
import world.phantasmal.web.questEditor.widgets.QuestEditorRendererWidget
import world.phantasmal.web.questEditor.widgets.QuestEditorToolbar
import world.phantasmal.web.questEditor.widgets.QuestEditorWidget

class QuestEditor(
    scope: Scope,
    uiStore: UiStore,
    createEngine: (HTMLCanvasElement) -> Engine,
) {
    private val toolbarController = QuestEditorToolbarController(scope)

    val widget = QuestEditorWidget(
        scope,
        QuestEditorToolbar(scope, toolbarController),
        { scope -> QuestEditorRendererWidget(scope, createEngine) }
    )
}
