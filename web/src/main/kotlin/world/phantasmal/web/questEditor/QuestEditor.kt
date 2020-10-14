package world.phantasmal.web.questEditor

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.core.disposable.Scope
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.externals.Engine
import world.phantasmal.web.questEditor.widgets.QuestEditorRendererWidget
import world.phantasmal.web.questEditor.widgets.QuestEditorWidget

class QuestEditor(
    scope: Scope,
    crScope: CoroutineScope,
    uiStore: UiStore,
    createEngine: (HTMLCanvasElement) -> Engine,
) {
    val widget = QuestEditorWidget(scope, { scope ->
        QuestEditorRendererWidget(scope, createEngine)
    })
}
