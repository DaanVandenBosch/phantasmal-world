package world.phantasmal.psolib.fileFormats.quest

import world.phantasmal.psolib.Episode

class Area(
    val id: Int, // Area ID
    val mapId: Int, // Map ID
    val name: String, // Area name
    val bossArea: Boolean, // Is this a boss area?
    val order: Int, // Order in episode
    val areaVariants: List<AreaVariant>, // Variants of this area
)

class AreaVariant(
    val id: Int, // Area variant ID
    val area: Area, // Parent area
)

fun getAreasForEpisode(episode: Episode): List<Area> =
    AREAS.getValue(episode)

private val AREAS by lazy {
    var order = 0

    @Suppress("UNUSED_CHANGED_VALUE")
    val ep1 = listOf(
        createArea(0, 0x00, "Pioneer II", order++, 1),
        createArea(1, 0x01, "Forest 1", order++, 1),
        createArea(2, 0x02, "Forest 2", order++, 1),
        createArea(3, 0x03, "Cave 1", order++, 6),
        createArea(4, 0x04, "Cave 2", order++, 5),
        createArea(5, 0x05, "Cave 3", order++, 6),
        createArea(6, 0x06, "Mine 1", order++, 6),
        createArea(7, 0x07, "Mine 2", order++, 6),
        createArea(8, 0x08, "Ruins 1", order++, 5),
        createArea(9, 0x09, "Ruins 2", order++, 5),
        createArea(10, 0x0A, "Ruins 3", order++, 5),
        createArea(11, 0x0B, "Under the Dome", order++, bossArea = true),
        createArea(12, 0x0C, "Underground Channel", order++, bossArea = true),
        createArea(13, 0x0D, "Monitor Room", order++, bossArea = true),
        createArea(14, 0x0E, "Dark Falz", order++, bossArea = true),
        createArea(15, 0x0F, "Lobby", order++, 15),
        createArea(16, 0x10, "BA Spaceship", order++, 3),
        createArea(17, 0x11, "BA Palace", order++, 3),
    )

    order = 0

    @Suppress("UNUSED_CHANGED_VALUE")
    val ep2 = listOf(
        createArea(0, 0x12, "Lab", order++, 1),
        createArea(1, 0x13, "VR Temple Alpha", order++, 3),
        createArea(2, 0x14, "VR Temple Beta", order++, 3),
        createArea(3, 0x15, "VR Spaceship Alpha", order++, 3),
        createArea(4, 0x16, "VR Spaceship Beta", order++, 3),
        createArea(5, 0x17, "Central Control Area", order++, 1),
        createArea(6, 0x18, "Jungle Area East", order++, 1),
        createArea(7, 0x19, "Jungle Area North", order++, 1),
        createArea(8, 0x1A, "Mountain Area", order++, 3),
        createArea(9, 0x1B, "Seaside Area", order++, 1),
        createArea(10, 0x1C, "Seabed Upper Levels", order++, 3),
        createArea(11, 0x1D, "Seabed Lower Levels", order++, 3),
        createArea(12, 0x1E, "Cliffs of Gal Da Val", order++, bossArea = true),
        createArea(13, 0x1F, "Test Subject Disposal Area", order++, bossArea = true),
        createArea(14, 0x20, "VR Temple Final", order++, bossArea = true),
        createArea(15, 0x21, "VR Spaceship Final", order++, bossArea = true),
        createArea(16, 0x22, "Seaside Area at Night", order++, 2),
        createArea(17, 0x23, "Tower", order++, 5),
    )

    order = 0

    @Suppress("UNUSED_CHANGED_VALUE")
    val ep4 = listOf(
        createArea(0, 0x2D, "Pioneer II", order++, 1),
        createArea(1, 0x24, "Crater Route 1", order++, 1),
        createArea(2, 0x25, "Crater Route 2", order++, 1),
        createArea(3, 0x26, "Crater Route 3", order++, 1),
        createArea(4, 0x27, "Crater Route 4", order++, 1),
        createArea(5, 0x28, "Crater Interior", order++, 1),
        createArea(6, 0x29, "Subterranean Desert 1", order++, 3),
        createArea(7, 0x2A, "Subterranean Desert 2", order++, 3),
        createArea(8, 0x2B, "Subterranean Desert 3", order++, 3),
        createArea(9, 0x2C, "Meteor Impact Site", order++, bossArea = true),
    )

    mapOf(
        Episode.I to ep1,
        Episode.II to ep2,
        Episode.IV to ep4,
    )
}

private fun createArea(
    id: Int,
    mapId: Int,
    name: String,
    order: Int,
    variants: Int = 1,
    bossArea: Boolean = false,
): Area {
    val avs = mutableListOf<AreaVariant>()
    val area = Area(id, mapId, name, bossArea, order, avs)
    repeat(variants) { avs.add(AreaVariant(it, area)) }
    return area
}

/**
 * Cache of all areas indexed by mapId for quick lookup.
 */
private val areasByMapId: Map<Int, Area> by lazy {
    (getAreasForEpisode(Episode.I) +
     getAreasForEpisode(Episode.II) +
     getAreasForEpisode(Episode.IV))
        .associateBy { it.mapId }
}

/**
 * Cache of all areas indexed by (episode, areaId) for quick lookup.
 * Episode is represented as Int: 0=I, 1=II, 2=IV
 */
private val areasByEpisodeAndId: Map<Pair<Int, Int>, Area> by lazy {
    listOf(0 to Episode.I, 1 to Episode.II, 2 to Episode.IV)
        .flatMap { (epInt, ep) -> getAreasForEpisode(ep).map { (epInt to it.id) to it } }
        .toMap()
}

/**
 * Finds an Area by its mapId.
 * @param mapId The game-internal map ID
 * @return The corresponding Area or null if not found
 */
fun findAreaByMapId(mapId: Int): Area? = areasByMapId[mapId]

/**
 * Finds an Area by episode and areaId.
 * @param episode The episode number (0=I, 1=II, 2=IV)
 * @param areaId The area ID within the episode
 * @return The corresponding Area or null if not found
 */
fun findAreaByEpisodeAndAreaId(episode: Int, areaId: Int): Area? =
    areasByEpisodeAndId[episode to areaId]

/**
 * Maps game-internal map ID to model area ID.
 * @param mapId The game-internal map ID
 * @return The area ID or null if not found
 */
fun getAreaIdByMapId(mapId: Int): Int? = findAreaByMapId(mapId)?.id

/**
 * Maps episode and area ID to map ID.
 * @param episode The episode number (0=I, 1=II, 2=IV)
 * @param areaId The area ID within the episode
 * @return The map ID or 0 if not found
 */
fun getMapId(episode: Int, areaId: Int): Int =
    findAreaByEpisodeAndAreaId(episode, areaId)?.mapId ?: 0

/**
 * Check if current area is a boss area
 */
fun isBossArea(episode: Episode, areaId: Int): Boolean =
    AREAS[episode]?.any { it.id == areaId && it.bossArea } ?: false

/**
 * Check if area is Pioneer 2 (Episode I) or Lab (Episode II)
 */
fun isPioneer2OrLab(areaId: Int): Boolean = areaId == 0
