package world.phantasmal.psolib.fileFormats.quest

import world.phantasmal.psolib.Episode

/**
 * Dat file type for free roam area data files.
 */
enum class DatFileType(val suffix: String) {
    /** Object data (doors, switches, decorative objects, etc.) */
    OBJECTS("o"),
    /** Enemy/NPC data */
    ENEMIES("e"),
    /** Event data (EP4 only) */
    EVENTS(""),
}

/**
 * Describes the dat file naming for a single floor in BB free roam mode.
 * Data sourced from newserv Map.cc `map_file_info` (BB online/non-solo).
 *
 * @param token Name token used in the dat filename (e.g., "forest01", "cave01")
 * @param v1Values Available layout variation values (empty = no v1 dimension in filename)
 * @param v2Values Available entity variation values (empty = no v2 dimension in filename)
 */
class FloorFileInfo(
    val token: String,
    val v1Values: List<Int>,
    val v2Values: List<Int>,
)

/**
 * Resolve a dat filename for a given floor's file info and variation indices.
 *
 * Naming patterns (from newserv SetDataTable):
 * - v1 present, v2 present: `map_{token}_{v1:02d}_{v2:02d}{type}.dat`
 * - v1 absent, v2 present:  `map_{token}_{v2:02d}{type}.dat`
 * - both absent:             `map_{token}{type}.dat`
 *
 * Where {type} is "o" (objects), "e" (enemies), or "" (events, uses .evt extension).
 */
fun resolveDatFilename(
    info: FloorFileInfo,
    v1: Int,
    v2: Int,
    type: DatFileType,
): String = buildString {
    append("map_")
    append(info.token)
    if (info.v1Values.isNotEmpty()) {
        append("_")
        append(v1.toString().padStart(2, '0'))
    }
    if (info.v2Values.isNotEmpty()) {
        append("_")
        append(v2.toString().padStart(2, '0'))
    }
    append(type.suffix)
    if (type == DatFileType.EVENTS) append(".evt") else append(".dat")
}

/**
 * Get floor file info for a given episode and floor number.
 * Returns null for floors without dat files (boss, lobby, BA areas).
 */
fun getFloorFileInfo(episode: Episode, floor: Int): FloorFileInfo? =
    when (episode) {
        Episode.I -> EP1_FLOORS.getOrNull(floor)
        Episode.II -> EP2_FLOORS.getOrNull(floor)
        Episode.IV -> EP4_FLOORS.getOrNull(floor)
    }

/**
 * Get all floor file info entries for an episode.
 * Indexed by floor number. Some entries may be null (boss/lobby floors).
 */
fun getAllFloorFileInfo(episode: Episode): List<FloorFileInfo?> =
    when (episode) {
        Episode.I -> EP1_FLOORS
        Episode.II -> EP2_FLOORS
        Episode.IV -> EP4_FLOORS
    }

// ---- BB online (non-solo) data from newserv Map.cc map_file_info ----

private fun f(token: String, v1: List<Int> = emptyList(), v2: List<Int> = emptyList()) =
    FloorFileInfo(token, v1, v2)

private val EP1_FLOORS: List<FloorFileInfo?> = listOf(
    f("city00", v2 = listOf(0)),                         // 0:  Pioneer II
    f("forest01", v2 = listOf(0, 1, 2, 3, 4)),           // 1:  Forest 1
    f("forest02", v2 = listOf(0, 1, 2, 3, 4)),           // 2:  Forest 2
    f("cave01", listOf(0, 1, 2), listOf(0, 1)),           // 3:  Cave 1
    f("cave02", listOf(0, 1, 2), listOf(0, 1)),           // 4:  Cave 2
    f("cave03", listOf(0, 1, 2), listOf(0, 1)),           // 5:  Cave 3
    f("machine01", listOf(0, 1, 2), listOf(0, 1)),        // 6:  Mine 1
    f("machine02", listOf(0, 1, 2), listOf(0, 1)),        // 7:  Mine 2
    f("ancient01", listOf(0, 1, 2), listOf(0, 1)),        // 8:  Ruins 1
    f("ancient02", listOf(0, 1, 2), listOf(0, 1)),        // 9:  Ruins 2
    f("ancient03", listOf(0, 1, 2), listOf(0, 1)),        // 10: Ruins 3
    null,                                                  // 11: Under the Dome (boss)
    null,                                                  // 12: Underground Channel (boss)
    null,                                                  // 13: Monitor Room (boss)
    null,                                                  // 14: Dark Falz (boss)
    null,                                                  // 15: Lobby
)

private val EP2_FLOORS: List<FloorFileInfo?> = listOf(
    f("labo00", v2 = listOf(0)),                          // 0:  Lab
    f("ruins01", listOf(0, 1), listOf(0)),                 // 1:  VR Temple Alpha
    f("ruins02", listOf(0, 1), listOf(0)),                 // 2:  VR Temple Beta
    f("space01", listOf(0, 1), listOf(0)),                 // 3:  VR Spaceship Alpha
    f("space02", listOf(0, 1), listOf(0)),                 // 4:  VR Spaceship Beta
    f("jungle01", v2 = listOf(0, 1, 2)),                  // 5:  CCA
    f("jungle02", v2 = listOf(0, 1, 2)),                  // 6:  Jungle East
    f("jungle03", v2 = listOf(0, 1, 2)),                  // 7:  Jungle North
    f("jungle04", listOf(0, 1), listOf(0, 1)),             // 8:  Mountain
    f("jungle05", v2 = listOf(0, 1, 2)),                  // 9:  Seaside
    f("seabed01", listOf(0, 1), listOf(0, 1)),             // 10: Seabed Upper
    f("seabed02", listOf(0, 1), listOf(0, 1)),             // 11: Seabed Lower
    null,                                                  // 12: Cliffs of Gal Da Val (boss)
    null,                                                  // 13: Test Subject Disposal (boss)
    null,                                                  // 14: VR Temple Final (boss)
    null,                                                  // 15: VR Spaceship Final (boss)
    f("jungle06", v2 = listOf(0, 1, 2)),                  // 16: Seaside Area at Night
    f("tower", listOf(0, 1), listOf(0)),                  // 17: Tower
)

private val EP4_FLOORS: List<FloorFileInfo?> = listOf(
    f("city02", listOf(0), listOf(0)),                     // 0:  Pioneer II
    f("wilds01", listOf(0), listOf(0, 1, 2)),              // 1:  Crater Route 1
    f("wilds01", listOf(1), listOf(0, 1, 2)),              // 2:  Crater Route 2
    f("wilds01", listOf(2), listOf(0, 1, 2)),              // 3:  Crater Route 3
    f("wilds01", listOf(3), listOf(0, 1, 2)),              // 4:  Crater Route 4
    f("crater01", listOf(0), listOf(0, 1, 2)),             // 5:  Crater Interior
    f("desert01", listOf(0, 1, 2), listOf(0)),             // 6:  Desert 1
    f("desert02", listOf(0), listOf(0, 1, 2)),             // 7:  Desert 2
    f("desert03", listOf(0, 1, 2), listOf(0)),             // 8:  Desert 3
    null,                                                  // 9:  Meteor Impact Site (boss)
)
