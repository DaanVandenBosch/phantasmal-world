package world.phantasmal.web.questEditor.stores

import kotlinx.coroutines.CoroutineScope
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.web.questEditor.models.QuestModel
import world.phantasmal.webui.stores.Store

class QuestEditorStore(scope: CoroutineScope) : Store(scope) {
    private val _currentQuest = mutableVal<QuestModel?>(null)

    val currentQuest: Val<QuestModel?> = _currentQuest

    fun setCurrentQuest(quest: QuestModel?) {
        _currentQuest.value = quest
    }
}
