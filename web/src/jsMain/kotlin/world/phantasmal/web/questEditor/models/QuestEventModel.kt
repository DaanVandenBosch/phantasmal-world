package world.phantasmal.web.questEditor.models

import world.phantasmal.cell.Cell
import world.phantasmal.cell.list.ListCell
import world.phantasmal.cell.list.mutableListCell
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
    cmWaveSettings: Int? = null,
) {
    private val _id = mutableCell(id)
    private val _sectionId = mutableCell(sectionId)
    private val _waveId = mutableCell(waveId)
    private val _delay = mutableCell(delay)
    private val _actions = mutableListCell(actions)
    private val _cmWaveSettings = mutableCell(cmWaveSettings)

    val id: Cell<Int> = _id
    val sectionId: Cell<Int> = _sectionId
    val wave: Cell<WaveModel> = map(_waveId, _sectionId) { id, sectionId ->
        WaveModel(id, areaId, sectionId)
    }
    val delay: Cell<Int> = _delay
    val actions: ListCell<QuestEventActionModel> = _actions
    val cmWaveSettings: Cell<Int?> = _cmWaveSettings

    // Challenge mode wave settings - decoded from cmWaveSettings
    val cmMinEnemies: Cell<Int> = _cmWaveSettings.map { it?.let { v -> v and 0xFF } ?: 0 }
    val cmMaxEnemies: Cell<Int> = _cmWaveSettings.map { it?.let { v -> (v shr 8) and 0xFF } ?: 0 }
    val cmMaxWaves: Cell<Int> = _cmWaveSettings.map { it?.let { v -> (v shr 16) and 0xFF } ?: 0 }

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

    fun setCmMinEnemies(value: Int) {
        val current = _cmWaveSettings.value ?: 0
        _cmWaveSettings.value = (current and 0xFFFFFF00.toInt()) or (value and 0xFF)
    }

    fun setCmMaxEnemies(value: Int) {
        val current = _cmWaveSettings.value ?: 0
        _cmWaveSettings.value = (current and 0xFFFF00FF.toInt()) or ((value and 0xFF) shl 8)
    }

    fun setCmMaxWaves(value: Int) {
        val current = _cmWaveSettings.value ?: 0
        _cmWaveSettings.value = (current and 0xFF00FFFF.toInt()) or ((value and 0xFF) shl 16)
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
