package world.phantasmal.web.questEditor.models

import world.phantasmal.cell.Cell
import world.phantasmal.cell.map
import world.phantasmal.cell.mutableCell
import world.phantasmal.psolib.fileFormats.quest.NpcType
import world.phantasmal.psolib.fileFormats.quest.QuestNpc
import world.phantasmal.web.externals.three.Vector3

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

    override val worldPosition: Cell<Vector3> =
        map(super.worldPosition, _spawnOnGround, section) { basePos, spawnOnGround, section ->
            val adjustedPos = if (spawnOnGround && section != null) {
                // Calculate the actual ground height for this position
                val groundY = calculateGroundHeight(basePos.x, basePos.z, section)
                Vector3(basePos.x, groundY, basePos.z)
            } else {
                basePos
            }

            // Apply entity-specific Y offset adjustments
            val yOffset = when (type) {
                NpcType.Epsilon -> 20.0  // Epsilon appears too low, raise by 20 units
                NpcType.GiGue -> 25.0    // Gigue appears too low, raise by 25 units
                NpcType.Bulclaw -> 25.0    // Bulclaw appears too low, raise by 25 units
                NpcType.Claw -> 10.0    // Claw appears too low, raise by 25 units
                NpcType.ChaosSorcerer -> 25.0    // ChaosSorcerer appears too low, raise by 25 units
                NpcType.ChaosSorcerer2 -> 25.0    // ChaosSorcerer(EP2) appears too low, raise by 25 units
                // Add more entity-specific Y offsets here as needed
                else -> 0.0
            }

            if (yOffset != 0.0) {
                Vector3(adjustedPos.x, adjustedPos.y + yOffset, adjustedPos.z)
            } else {
                adjustedPos
            }
        }

    private fun calculateGroundHeight(x: Double, z: Double, section: SectionModel): Double {
        // Try to get ground height from the provided ground height calculator
        // If not available, fall back to section position
        return _groundHeightCalculator?.invoke(x, z, section) ?: section.position.y
    }

    companion object {
        private val _spawnOnGround = mutableCell(false)
        private var _groundHeightCalculator: ((x: Double, z: Double, section: SectionModel) -> Double)? = null

        fun setSpawnOnGround(spawn: Boolean) {
            _spawnOnGround.value = spawn
        }

        fun spawnOnGround(): Boolean = _spawnOnGround.value

        fun setGroundHeightCalculator(calculator: ((x: Double, z: Double, section: SectionModel) -> Double)?) {
            _groundHeightCalculator = calculator
        }
    }
}
