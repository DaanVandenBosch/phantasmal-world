package world.phantasmal.psolib.compatibility

import world.phantasmal.psolib.Episode

/**
 * Provides floor-specific data for compatibility checking.
 * This interface allows different implementations for different data sources.
 */
interface FloorDataProvider {
    /**
     * Get the list of allowed monster IDs for a specific floor and version.
     * Returns null if no restrictions apply (all monsters allowed).
     */
    fun getFloorMonsters(floorId: Int, version: Int): List<Int>?

    /**
     * Get the list of allowed object IDs for a specific floor and version.
     * Returns null if no restrictions apply (all objects allowed).
     */
    fun getFloorObjects(floorId: Int, version: Int): List<Int>?

    /**
     * Validate NPC skin 51 subtype for a specific floor.
     */
    fun isValidNPC51(floorId: Int, subtype: Int): Boolean
}

/**
 * Default implementation that doesn't restrict any monsters/objects.
 */
const val MAX_SKIN_51_SUBTYPE = 15

object NoRestrictionFloorDataProvider : FloorDataProvider {
    override fun getFloorMonsters(floorId: Int, version: Int): List<Int>? = null
    override fun getFloorObjects(floorId: Int, version: Int): List<Int>? = null
    override fun isValidNPC51(floorId: Int, subtype: Int): Boolean = subtype <= MAX_SKIN_51_SUBTYPE
}

/**
 * Default NPC action labels for Pioneer 2/Lab.
 *
 * - Base labels (EP1_BASE/EP2_BASE) are available in all versions
 * - Extended labels (EP1_EXTRA/EP2_EXTRA) are built-in only for GC Ep1&2 (ver=3)
 * - Other versions (DC, PC, BB) require these labels to be defined in the quest script
 *
 * If an NPC uses a non-default label, it must be defined in the quest script.
 */
object DefaultLabels {
    // Base labels for menu activation functionality (available in all versions)
    // Episode 1/4: indices 0-12
    val EP1_BASE = setOf(100, 90, 120, 130, 80, 70, 60, 140, 110, 30, 50, 1, 20)

    // Extended labels for Episode 1/4: indices 13-21 (built-in on GC only)
    val EP1_EXTRA = setOf(850, 800, 830, 820, 810, 860, 870, 840, 880)

    // Episode 2: indices 0-9
    val EP2_BASE = setOf(720, 660, 620, 600, 501, 520, 560, 540, 580, 680)

    // Extended labels for Episode 2: indices 10-18 (built-in on GC only)
    val EP2_EXTRA = setOf(950, 900, 930, 920, 910, 960, 970, 940, 980)

    /**
     * Enemy type IDs (not NPCs).
     */
    val ENEMY_IDS = setOf(
        68, 67, 64, 65, 128, 129, 131, 133, 163, 97, 99, 98, 96,
        168, 166, 165, 160, 162, 164, 192, 197, 193, 194, 200,
        66, 132, 130, 100, 101, 161, 167, 223, 213, 212, 215,
        217, 218, 214, 222, 221, 225, 224, 216, 219, 220, 202,
        201, 203, 204, 273, 277, 276, 272, 278, 274, 275, 281, 249
    )

    /**
     * Check if a label is a base label for the given episode.
     * Base labels are built-in for all PSO versions.
     */
    fun isBaseLabel(label: Int, episode: Episode): Boolean = when (episode) {
        Episode.II -> label in EP2_BASE
        else -> label in EP1_BASE  // Episode I and IV
    }

    /**
     * Check if a label is an extra label for the given episode.
     * Extra labels are only built-in for GC Ep1&2 (ver=3).
     * For other versions (including BB which uses DC V2 rules),
     * extra labels must be defined in the quest script.
     */
    fun isExtraLabel(label: Int, episode: Episode): Boolean = when (episode) {
        Episode.II -> label in EP2_EXTRA
        else -> label in EP1_EXTRA  // Episode I and IV
    }
}