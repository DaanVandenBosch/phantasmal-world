package world.phantasmal.psolib.fileFormats.quest

import world.phantasmal.psolib.Episode

class Area(
    val id: Int,
    val name: String,
    val bossArea: Boolean,
    val order: Int,
    val areaVariants: List<AreaVariant>,
)

class AreaVariant(
    val id: Int,
    val area: Area,
)

fun getAreasForEpisode(episode: Episode): List<Area> = AREAS.getValue(episode)

private val AREAS by lazy {
    var order = 0

    @Suppress("UNUSED_CHANGED_VALUE") val ep1 = listOf(
        createArea(0, "Pioneer II", order++, 1),
        createArea(1, "Forest 1", order++, 1),
        createArea(2, "Forest 2", order++, 1),
        createArea(11, "Under the Dome", true, order++, 1),
        createArea(3, "Cave 1", order++, 6),
        createArea(4, "Cave 2", order++, 5),
        createArea(5, "Cave 3", order++, 6),
        createArea(12, "Underground Channel", true, order++, 1),
        createArea(6, "Mine 1", order++, 6),
        createArea(7, "Mine 2", order++, 6),
        createArea(13, "Monitor Room", true, order++, 1),
        createArea(8, "Ruins 1", order++, 5),
        createArea(9, "Ruins 2", order++, 5),
        createArea(10, "Ruins 3", order++, 5),
        createArea(14, "Dark Falz", true, order++, 1),
        createArea(15, "Lobby", order++, 15),
        createArea(16, "BA Spaceship", order++, 3),
        createArea(17, "BA Palace", order++, 3),
    )

    order = 0

    @Suppress("UNUSED_CHANGED_VALUE") val ep2 = listOf(
        createArea(0, "Lab", order++, 1),
        createArea(1, "VR Temple Alpha", order++, 3),
        createArea(2, "VR Temple Beta", order++, 3),
        createArea(14, "VR Temple Final", true, order++, 1),
        createArea(3, "VR Spaceship Alpha", order++, 3),
        createArea(4, "VR Spaceship Beta", order++, 3),
        createArea(15, "VR Spaceship Final", true, order++, 1),
        createArea(5, "Central Control Area", order++, 1),
        createArea(6, "Jungle Area East", order++, 1),
        createArea(7, "Jungle Area North", order++, 1),
        createArea(8, "Mountain Area", order++, 3),
        createArea(9, "Seaside Area", order++, 1),
        createArea(12, "Cliffs of Gal Da Val", true, order++, 1),
        createArea(10, "Seabed Upper Levels", order++, 3),
        createArea(11, "Seabed Lower Levels", order++, 3),
        createArea(13, "Test Subject Disposal Area", true, order++, 1),
        createArea(16, "Seaside Area at Night", order++, 2),
        createArea(17, "Tower", order++, 5),
    )

    order = 0

    @Suppress("UNUSED_CHANGED_VALUE") val ep4 = listOf(
        createArea(0, "Pioneer II", order++, 1),
        createArea(1, "Crater Route 1", order++, 1),
        createArea(2, "Crater Route 2", order++, 1),
        createArea(3, "Crater Route 3", order++, 1),
        createArea(4, "Crater Route 4", order++, 1),
        createArea(5, "Crater Interior", order++, 1),
        createArea(6, "Subterranean Desert 1", order++, 3),
        createArea(7, "Subterranean Desert 2", order++, 3),
        createArea(8, "Subterranean Desert 3", order++, 3),
        createArea(9, "Meteor Impact Site", true, order++, 1),
    )

    mapOf(
        Episode.I to ep1,
        Episode.II to ep2,
        Episode.IV to ep4,
    )
}

private fun createArea(id: Int, name: String, order: Int, variants: Int): Area {
    return createArea(id, name, false, order, variants)
}

private fun createArea(id: Int, name: String, bossArea: Boolean, order: Int, variants: Int): Area {
    val avs = mutableListOf<AreaVariant>()
    val area = Area(id, name, bossArea, order, avs)

    for (avId in 0 until variants) {
        avs.add(AreaVariant(avId, area))
    }

    return area
}

/**
 * Check if current area is a boss area
 */
fun isBossArea(episode: Episode, areaId: Int): Boolean {
    AREAS[episode]?.any { area -> area.id == areaId && area.bossArea }.let { return it == true }
}

fun isPioneer2OrLab(episode: Episode, areaId: Int): Boolean {
    return when (episode) {
        Episode.I -> areaId == 0  // EP1 Pioneer II
        Episode.II -> areaId == 0 // EP2 Lab
        Episode.IV -> areaId == 0 // EP4 Pioneer II
    }
}
