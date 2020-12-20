package world.phantasmal.web.questEditor.models

import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.list.mutableListVal
import world.phantasmal.observable.value.mutableVal

class QuestEventModel(
    id: Int,
    val areaId: Int,
    val sectionId: Int,
    wave: WaveModel,
    delay: Int,
    val unknown: Int,
    actions: MutableList<QuestEventActionModel>,
) {
    private val _id = mutableVal(id)
    private val _wave = mutableVal(wave)
    private val _delay = mutableVal(delay)
    private val _actions = mutableListVal(actions)

    val id: Val<Int> = _id
    val wave: Val<WaveModel> = _wave
    val delay: Val<Int> = _delay
    val actions: ListVal<QuestEventActionModel> = _actions

    fun setId(id: Int) {
        _id.value = id
    }

    fun setWaveId(waveId: Int) {
        _wave.value = WaveModel(waveId, areaId, sectionId)
    }

    fun setDelay(delay: Int) {
        _delay.value = delay
    }

    fun addAction(action: QuestEventActionModel) {
        _actions.add(action)
    }

    fun removeAction(action: QuestEventActionModel) {
        _actions.remove(action)
    }
}
