package world.phantasmal.web.questEditor.models

import world.phantasmal.lib.fileFormats.quest.NpcType
import world.phantasmal.lib.fileFormats.quest.QuestNpc
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.map
import world.phantasmal.observable.value.mutableVal

class QuestNpcModel(npc: QuestNpc, waveId: Int) : QuestEntityModel<NpcType, QuestNpc>(npc) {
    private val _waveId = mutableVal(waveId)

    val wave: Val<WaveModel> = map(_waveId, sectionId) { id, sectionId ->
        WaveModel(id, areaId, sectionId)
    }

    fun setWaveId(waveId: Int) {
        entity.wave = waveId.toShort()
        entity.wave2 = waveId
        _waveId.value = waveId
    }
}
