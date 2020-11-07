package world.phantasmal.web.questEditor.loading

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import org.khronos.webgl.ArrayBuffer
import world.phantasmal.lib.Endianness
import world.phantasmal.lib.cursor.cursor
import world.phantasmal.lib.fileFormats.parseAreaCollisionGeometry
import world.phantasmal.lib.fileFormats.quest.Episode
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.externals.babylon.Scene
import world.phantasmal.web.externals.babylon.TransformNode
import world.phantasmal.web.questEditor.models.AreaVariantModel
import world.phantasmal.web.questEditor.rendering.conversion.areaCollisionGeometryToTransformNode
import world.phantasmal.webui.DisposableContainer

class AreaAssetLoader(
    private val scope: CoroutineScope,
    private val assetLoader: AssetLoader,
    private val scene: Scene,
) : DisposableContainer() {
    private val collisionObjectCache =
        addDisposable(LoadingCache<Triple<Episode, Int, Int>, TransformNode> { it.dispose() })

    suspend fun loadCollisionGeometry(
        episode: Episode,
        areaVariant: AreaVariantModel,
    ): TransformNode =
        collisionObjectCache.getOrPut(Triple(episode, areaVariant.area.id, areaVariant.id)) {
            scope.async {
                val buffer = getAreaAsset(episode, areaVariant, AssetType.Collision)
                val obj = parseAreaCollisionGeometry(buffer.cursor(Endianness.Little))
                areaCollisionGeometryToTransformNode(scene, obj)
            }
        }.await()

    private suspend fun getAreaAsset(
        episode: Episode,
        areaVariant: AreaVariantModel,
        type: AssetType,
    ): ArrayBuffer {
        val baseUrl = areaVersionToBaseUrl(episode, areaVariant)
        val suffix = when (type) {
            AssetType.Render -> "n.rel"
            AssetType.Collision -> "c.rel"
        }
        return assetLoader.loadArrayBuffer(baseUrl + suffix)
    }

    enum class AssetType {
        Render, Collision
    }
}

private val AREA_BASE_NAMES: Map<Episode, List<Pair<String, Int>>> = mapOf(
    Episode.I to listOf(
        Pair("city00_00", 1),
        Pair("forest01", 1),
        Pair("forest02", 1),
        Pair("cave01_", 6),
        Pair("cave02_", 5),
        Pair("cave03_", 6),
        Pair("machine01_", 6),
        Pair("machine02_", 6),
        Pair("ancient01_", 5),
        Pair("ancient02_", 5),
        Pair("ancient03_", 5),
        Pair("boss01", 1),
        Pair("boss02", 1),
        Pair("boss03", 1),
        Pair("darkfalz00", 1),
    ),
    Episode.II to listOf(
        Pair("labo00_00", 1),
        Pair("ruins01_", 3),
        Pair("ruins02_", 3),
        Pair("space01_", 3),
        Pair("space02_", 3),
        Pair("jungle01_00", 1),
        Pair("jungle02_00", 1),
        Pair("jungle03_00", 1),
        Pair("jungle04_", 3),
        Pair("jungle05_00", 1),
        Pair("seabed01_", 3),
        Pair("seabed02_", 3),
        Pair("boss05", 1),
        Pair("boss06", 1),
        Pair("boss07", 1),
        Pair("boss08", 1),
        Pair("jungle06_00", 1),
        Pair("jungle07_", 5),
    ),
    Episode.IV to listOf(
        Pair("city02_00", 1),
        Pair("wilds01_00", 1),
        Pair("wilds01_01", 1),
        Pair("wilds01_02", 1),
        Pair("wilds01_03", 1),
        Pair("crater01_00", 1),
        Pair("desert01_", 3),
        Pair("desert02_", 3),
        Pair("desert03_", 3),
        Pair("boss09_00", 1),
    )
)

private fun areaVersionToBaseUrl(episode: Episode, areaVariant: AreaVariantModel): String {
    var areaId = areaVariant.area.id
    var areaVariantId = areaVariant.id

    // Exception for Seaside area at night, variant 1.
    // Phantasmal World 4 and Lost heart breaker use this to have two tower maps.
    if (areaId == 16 && areaVariantId == 1) {
        areaId = 17
        areaVariantId = 1
    }

    val episodeBaseNames = AREA_BASE_NAMES.getValue(episode)

    require(areaId in episodeBaseNames.indices) {
        "Unknown episode $episode area $areaId."
    }

    val (base_name, variants) = episodeBaseNames[areaId]

    require(areaVariantId in 0 until variants) {
        "Unknown variant $areaVariantId of area $areaId in episode $episode."
    }

    val variant = if (variants == 1) {
        ""
    } else {
        areaVariantId.toString().padStart(2, '0')
    }

    return "/maps/map_${base_name}${variant}"
}
