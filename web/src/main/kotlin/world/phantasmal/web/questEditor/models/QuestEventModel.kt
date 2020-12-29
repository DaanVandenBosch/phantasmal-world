package world.phantasmal.web.questEditor.models

import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.list.SimpleListVal
import world.phantasmal.observable.value.map
import world.phantasmal.observable.value.mutableVal

class QuestEventModel(
    id: Int,
    val areaId: Int,
    sectionId: Int,
    waveId: Int,
    delay: Int,
    val unknown: Int,
    actions: MutableList<QuestEventActionModel>,
) {
    private val _id = mutableVal(id)
    private val _sectionId = mutableVal(sectionId)
    private val _waveId = mutableVal(waveId)
    private val _delay = mutableVal(delay)
    private val _actions = SimpleListVal(actions)

    val id: Val<Int> = _id
    val sectionId: Val<Int> = _sectionId
    val wave: Val<WaveModel> = map(_waveId, _sectionId) { id, sectionId ->
        WaveModel(id, areaId, sectionId)
    }
    val delay: Val<Int> = _delay
    val actions: ListVal<QuestEventActionModel> = _actions

    fun setId(id: Int) {
        _id.value = id
    }

    fun setSectionId(sectionId: Int) {
        _sectionId.value = sectionId
    }

    fun setWaveId(waveId: Int) {
        _waveId.value = waveId
    }

    fun setDelay(delay: Int) {
        _delay.value = delay
    }

    fun addAction(action: QuestEventActionModel) {
        _actions.add(action)
    }

    fun addAction(index: Int, action: QuestEventActionModel) {
        _actions.add(index, action)
    }

    fun removeAction(action: QuestEventActionModel) {
        _actions.remove(action)
    }
}
