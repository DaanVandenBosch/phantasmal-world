package world.phantasmal.web.questEditor.models

import world.phantasmal.cell.Cell
import world.phantasmal.cell.list.ListCell
import world.phantasmal.cell.list.SimpleListCell
import world.phantasmal.cell.map
import world.phantasmal.cell.mutableCell

class QuestEventModel(
    id: Int,
    val areaId: Int,
    sectionId: Int,
    waveId: Int,
    delay: Int,
    val unknown: Int,
    actions: MutableList<QuestEventActionModel>,
) {
    private val _id = mutableCell(id)
    private val _sectionId = mutableCell(sectionId)
    private val _waveId = mutableCell(waveId)
    private val _delay = mutableCell(delay)
    private val _actions = SimpleListCell(actions)

    val id: Cell<Int> = _id
    val sectionId: Cell<Int> = _sectionId
    val wave: Cell<WaveModel> = map(_waveId, _sectionId) { id, sectionId ->
        WaveModel(id, areaId, sectionId)
    }
    val delay: Cell<Int> = _delay
    val actions: ListCell<QuestEventActionModel> = _actions

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
