package world.phantasmal.web.questEditor.models

import world.phantasmal.psolib.fileFormats.quest.NpcType
import world.phantasmal.psolib.fileFormats.quest.QuestNpc
import world.phantasmal.cell.Cell
import world.phantasmal.cell.map
import world.phantasmal.cell.mutableCell

class QuestNpcModel(npc: QuestNpc, waveId: Int) : QuestEntityModel<NpcType, QuestNpc>(npc) {
    private val _waveId = mutableCell(waveId)

    val wave: Cell<WaveModel> = map(_waveId, sectionId) { id, sectionId ->
        WaveModel(id, areaId, sectionId)
    }

    fun setWaveId(waveId: Int) {
        entity.wave = waveId.toShort()
        entity.wave2 = waveId
        _waveId.value = waveId
    }
}
