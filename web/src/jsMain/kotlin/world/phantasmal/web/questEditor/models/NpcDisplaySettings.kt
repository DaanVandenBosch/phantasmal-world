package world.phantasmal.web.questEditor.models

import world.phantasmal.cell.Cell
import world.phantasmal.cell.mutableCell

/**
 * Application-wide display settings that affect how NPCs are rendered.
 * Shared across all [QuestNpcModel] instances. Set by [QuestEditorStore] and
 * [EntityMeshManager]; read reactively by each NPC model's [worldPosition] cell.
 */
object NpcDisplaySettings {
    private val _spawnOnGround = mutableCell(false)

    val spawnOnGround: Cell<Boolean> = _spawnOnGround

    var groundHeightCalculator: ((x: Double, z: Double, section: SectionModel) -> Double)? = null
        private set

    fun setSpawnOnGround(value: Boolean) {
        _spawnOnGround.value = value
    }

    fun setGroundHeightCalculator(calculator: ((x: Double, z: Double, section: SectionModel) -> Double)?) {
        groundHeightCalculator = calculator
    }
}
