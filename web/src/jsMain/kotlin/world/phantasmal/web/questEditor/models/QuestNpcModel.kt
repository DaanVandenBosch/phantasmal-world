package world.phantasmal.web.questEditor.models

import world.phantasmal.cell.Cell
import world.phantasmal.cell.map
import world.phantasmal.cell.mutableCell
import world.phantasmal.psolib.fileFormats.quest.NpcType
import world.phantasmal.psolib.fileFormats.quest.QuestNpc
import world.phantasmal.web.externals.three.Vector3

class QuestNpcModel(npc: QuestNpc, waveId: Int) : QuestEntityModel<NpcType, QuestNpc>(npc) {
    private val _waveId = mutableCell(waveId)

    /**
     * Whether this NPC is a StageNPC (typeId=0x33).
     *
     * StageNPC is a single NPC type that maps to many different monsters via subtype (character ID).
     * Its Y position in quest data differs from regular NPCs of the same monster type (e.g.,
     * StageNPC GiGue y=-15 vs regular GiGue y=0~3.8), because the game engine handles StageNPC
     * positioning differently. We ignore the data Y and use section ground height + yOffset instead.
     */
    private val isStageNpc: Boolean = npc.typeId.toInt() == 0x33

    /**
     * Y offset precomputed at construction time based on the resolved NPC type.
     * For StageNPC, the type is resolved from the NPC51 table via subtype/character ID.
     */
    private val yOffset: Double = computeYOffset(type)

    val wave: Cell<WaveModel> = map(_waveId, sectionId) { id, sectionId ->
        WaveModel(id, areaId, sectionId)
    }

    fun setWaveId(waveId: Int) {
        entity.wave = waveId.toShort()
        entity.wave2 = waveId
        _waveId.value = waveId
    }

    override val worldPosition: Cell<Vector3> =
        map(super.worldPosition, NpcDisplaySettings.spawnOnGround, section) { basePos, spawnOnGround, section ->
            if (isStageNpc && section != null) {
                val groundY = calculateGroundHeight(basePos.x, basePos.z, section)
                Vector3(basePos.x, groundY + yOffset, basePos.z)
            } else if (spawnOnGround && section != null) {
                val groundY = calculateGroundHeight(basePos.x, basePos.z, section)
                Vector3(basePos.x, groundY + yOffset, basePos.z)
            } else if (yOffset != 0.0) {
                Vector3(basePos.x, basePos.y + yOffset, basePos.z)
            } else {
                basePos
            }
        }

    private fun calculateGroundHeight(x: Double, z: Double, section: SectionModel): Double =
        NpcDisplaySettings.groundHeightCalculator?.invoke(x, z, section) ?: section.position.y

    companion object {
        /**
         * Y offset to prevent models from clipping into the ground when spawned at terrain height.
         * Values determined empirically by comparing editor placement with in-game appearance.
         */
        private fun computeYOffset(npcType: NpcType): Double = when (npcType) {
            NpcType.Epsilon -> 20.0
            NpcType.GiGue -> 25.0
            NpcType.ChaosSorcerer, NpcType.ChaosSorcerer2 -> 25.0
            NpcType.Bulclaw -> 25.0
            NpcType.Recon -> 10.0
            NpcType.Claw -> 10.0
            else -> 0.0
        }
    }
}
