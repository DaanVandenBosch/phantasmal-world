package world.phantasmal.web.questEditor.models

import world.phantasmal.lib.fileFormats.quest.NpcType
import world.phantasmal.lib.fileFormats.quest.QuestNpc
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.mutableVal

class QuestNpcModel(npc: QuestNpc, wave: WaveModel?) : QuestEntityModel<NpcType, QuestNpc>(npc) {
    private val _wave = mutableVal(wave)

    val wave: Val<WaveModel?> = _wave
}
