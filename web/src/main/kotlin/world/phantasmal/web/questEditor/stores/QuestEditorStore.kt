package world.phantasmal.web.questEditor.stores

import kotlinx.coroutines.CoroutineScope
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.web.questEditor.models.AreaModel
import world.phantasmal.web.questEditor.models.QuestModel
import world.phantasmal.web.questEditor.models.WaveModel
import world.phantasmal.webui.stores.Store

class QuestEditorStore(scope: CoroutineScope) : Store(scope) {
    private val _currentQuest = mutableVal<QuestModel?>(null)
    private val _currentArea = mutableVal<AreaModel?>(null)
    private val _selectedWave = mutableVal<WaveModel?>(null)

    val currentQuest: Val<QuestModel?> = _currentQuest
    val currentArea: Val<AreaModel?> = _currentArea
    val selectedWave: Val<WaveModel?> = _selectedWave

    // TODO: Take into account whether we're debugging or not.
    val questEditingDisabled: Val<Boolean> = currentQuest.map { it == null }

    fun setCurrentQuest(quest: QuestModel?) {
        _currentQuest.value = quest
    }
}
