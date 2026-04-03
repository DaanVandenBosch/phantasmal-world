package world.phantasmal.web.questEditor.models

import world.phantasmal.cell.Cell
import world.phantasmal.cell.mutableCell
import world.phantasmal.psolib.fileFormats.quest.DatCmRandomSpawnEntry
import world.phantasmal.web.externals.three.Vector3

/**
 * Model representing a challenge mode random monster spawn point.
 * These are visualized but not selectable/editable.
 */
class ChallengeMonsterSpawnModel(
    val areaId: Int,
    val entry: DatCmRandomSpawnEntry,
    section: SectionModel?,
) {
    private val _position = mutableCell(calculateWorldPosition(entry, section))

    private val _rotation = mutableCell(
        Vector3(
            0.0,
            entry.rotation.toDouble() * SCALE_FACTOR,  // Convert from 16-bit to radians
            0.0
        )
    )

    val position: Cell<Vector3> = _position
    val rotation: Cell<Vector3> = _rotation
    val sectionId: Int = entry.sectionId.toInt()

    companion object {
        // Scale factor to convert from 16-bit rotation to radians
        private const val SCALE_FACTOR = 2.0 * kotlin.math.PI / 65535.0

        /**
         * Calculate world position from section-relative position.
         * Uses the same transformation as QuestEntityModel: applyEuler + add position
         */
        private fun calculateWorldPosition(
            entry: DatCmRandomSpawnEntry,
            section: SectionModel?
        ): Vector3 {
            // Section-relative position
            val localPos = Vector3(
                entry.x.toDouble(),
                entry.unknown1.toDouble(),  // Height
                entry.y.toDouble()
            )

            if (section == null) {
                // If no section, use local coordinates directly
                return localPos
            }

            // Apply section transformation the same way as normal entities:
            // 1. Apply Euler rotation
            // 2. Add section position
            return localPos.clone().applyEuler(section.rotation).add(section.position)
        }
    }
}
