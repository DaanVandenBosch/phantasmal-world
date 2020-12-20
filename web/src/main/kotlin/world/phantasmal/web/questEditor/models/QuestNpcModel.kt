package world.phantasmal.web.questEditor.models

import world.phantasmal.lib.fileFormats.quest.NpcType
import world.phantasmal.lib.fileFormats.quest.QuestNpc
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.mutableVal

class QuestNpcModel(npc: QuestNpc, wave: WaveModel) : QuestEntityModel<NpcType, QuestNpc>(npc) {
    private val _wave = mutableVal(wave)

    val wave: Val<WaveModel> = _wave

    override fun setSectionId(sectionId: Int) {
        super.setSectionId(sectionId)

        if (sectionId != wave.value.sectionId) {
            _wave.value = WaveModel(wave.value.id, areaId, sectionId)
        }
    }

    fun setWaveId(waveId: Int) {
        entity.wave = waveId.toShort()
        entity.wave2 = waveId
        _wave.value = WaveModel(waveId, areaId, sectionId.value)
    }
}
