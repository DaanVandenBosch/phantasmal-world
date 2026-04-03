package world.phantasmal.psolib.fileFormats.quest

import world.phantasmal.psolib.Episode

class Area(
    val id: Int,
    val mapId: Int,
    val name: String,
    val bossArea: Boolean,
    val order: Int,
    val areaVariants: List<AreaVariant>,
)

class AreaVariant(
    val id: Int,
    val area: Area,
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

private val areasByMapId: Map<Int, Area> by lazy {
    (getAreasForEpisode(Episode.I) +
     getAreasForEpisode(Episode.II) +
     getAreasForEpisode(Episode.IV))
        .associateBy { it.mapId }
}

private val areasByEpisodeAndId: Map<Pair<Episode, Int>, Area> by lazy {
    listOf(Episode.I, Episode.II, Episode.IV)
        .flatMap { ep -> getAreasForEpisode(ep).map { (ep to it.id) to it } }
        .toMap()
}

private val episodeByMapId: Map<Int, Episode> by lazy {
    listOf(Episode.I, Episode.II, Episode.IV).flatMap { ep ->
        getAreasForEpisode(ep).map { it.mapId to ep }
    }.toMap()
}

fun findAreaByMapId(mapId: Int): Area? = areasByMapId[mapId]

fun findAreaByEpisodeAndAreaId(episode: Episode, areaId: Int): Area? =
    areasByEpisodeAndId[episode to areaId]

fun getAreaIdByMapId(mapId: Int): Int? = findAreaByMapId(mapId)?.id

fun getMapId(episode: Episode, areaId: Int): Int? =
    findAreaByEpisodeAndAreaId(episode, areaId)?.mapId

fun findEpisodeByMapId(mapId: Int): Episode? = episodeByMapId[mapId]

fun isBossArea(episode: Episode, areaId: Int): Boolean =
    AREAS[episode]?.any { it.id == areaId && it.bossArea } ?: false

fun isPioneer2OrLab(areaId: Int): Boolean = areaId == 0
