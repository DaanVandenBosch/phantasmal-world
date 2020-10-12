package world.phantasmal.web.questEditor

import kotlinx.coroutines.CoroutineScope
import world.phantasmal.core.disposable.DisposableContainer
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.questEditor.widgets.QuestEditorWidget

class QuestEditor(scope: CoroutineScope, uiStore: UiStore) : DisposableContainer() {
    val widget = QuestEditorWidget()
}
