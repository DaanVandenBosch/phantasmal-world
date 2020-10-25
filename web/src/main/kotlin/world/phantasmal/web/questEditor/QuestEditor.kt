package world.phantasmal.web.questEditor

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.externals.Engine
import world.phantasmal.web.questEditor.controllers.QuestEditorToolbarController
import world.phantasmal.web.questEditor.controllers.QuestInfoController
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.web.questEditor.widgets.QuestEditorRendererWidget
import world.phantasmal.web.questEditor.widgets.QuestEditorToolbar
import world.phantasmal.web.questEditor.widgets.QuestEditorWidget
import world.phantasmal.web.questEditor.widgets.QuestInfoWidget
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.widgets.Widget

class QuestEditor(
    private val scope: CoroutineScope,
    uiStore: UiStore,
    private val createEngine: (HTMLCanvasElement) -> Engine,
) : DisposableContainer() {
    private val questEditorStore = addDisposable(QuestEditorStore(scope))

    private val toolbarController =
        addDisposable(QuestEditorToolbarController(scope, questEditorStore))
    private val questInfoController = addDisposable(QuestInfoController(scope, questEditorStore))

    fun createWidget(): Widget =
        QuestEditorWidget(
            scope,
            QuestEditorToolbar(scope, toolbarController),
            { scope -> QuestInfoWidget(scope, questInfoController) },
            { scope -> QuestEditorRendererWidget(scope, createEngine) }
        )
}
