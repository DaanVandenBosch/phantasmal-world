package world.phantasmal.web.questEditor.loading

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import org.khronos.webgl.ArrayBuffer
import world.phantasmal.lib.Endianness
import world.phantasmal.lib.cursor.cursor
import world.phantasmal.lib.fileFormats.CollisionObject
import world.phantasmal.lib.fileFormats.RenderObject
import world.phantasmal.lib.fileFormats.parseAreaCollisionGeometry
import world.phantasmal.lib.fileFormats.parseAreaGeometry
import world.phantasmal.lib.fileFormats.quest.Episode
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.rendering.conversion.VertexDataBuilder
import world.phantasmal.web.core.rendering.conversion.ninjaObjectToVertexDataBuilder
import world.phantasmal.web.core.rendering.conversion.vec3ToBabylon
import world.phantasmal.web.externals.babylon.Mesh
import world.phantasmal.web.externals.babylon.Scene
import world.phantasmal.web.externals.babylon.TransformNode
import world.phantasmal.web.questEditor.models.AreaVariantModel
import world.phantasmal.web.questEditor.models.SectionModel
import world.phantasmal.webui.DisposableContainer

/**
 * Loads and caches area assets.
 */
class AreaAssetLoader(
    private val scope: CoroutineScope,
    private val assetLoader: AssetLoader,
    private val scene: Scene,
) : DisposableContainer() {
    /**
     * This cache's values consist of a TransformNode containing area render meshes and a list of
     * that area's sections.
     */
    private val renderObjectCache = addDisposable(
        LoadingCache<CacheKey, Pair<TransformNode, List<SectionModel>>> { it.first.dispose() }
    )

    private val collisionObjectCache = addDisposable(
        LoadingCache<CacheKey, TransformNode> { it.dispose() }
    )

    suspend fun loadSections(episode: Episode, areaVariant: AreaVariantModel): List<SectionModel> =
        loadRenderGeometryAndSections(episode, areaVariant).second

    suspend fun loadRenderGeometry(episode: Episode, areaVariant: AreaVariantModel): TransformNode =
        loadRenderGeometryAndSections(episode, areaVariant).first

    private suspend fun loadRenderGeometryAndSections(
        episode: Episode,
        areaVariant: AreaVariantModel,
    ): Pair<TransformNode, List<SectionModel>> =
        renderObjectCache.getOrPut(CacheKey(episode, areaVariant.area.id, areaVariant.id)) {
            scope.async {
                val buffer = getAreaAsset(episode, areaVariant, AssetType.Render)
                val obj = parseAreaGeometry(buffer.cursor(Endianness.Little))
                areaGeometryToTransformNodeAndSections(scene, obj, areaVariant)
            }
        }.await()

    suspend fun loadCollisionGeometry(
        episode: Episode,
        areaVariant: AreaVariantModel,
    ): TransformNode =
        collisionObjectCache.getOrPut(CacheKey(episode, areaVariant.area.id, areaVariant.id)) {
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

    private data class CacheKey(
        val episode: Episode,
        val areaId: Int,
        val areaVariantId: Int,
    )

    private enum class AssetType {
        Render, Collision
    }
}

class AreaMetadata(
    val section: SectionModel?,
)

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

private fun areaGeometryToTransformNodeAndSections(
    scene: Scene,
    renderObject: RenderObject,
    areaVariant: AreaVariantModel,
): Pair<TransformNode, List<SectionModel>> {
    val sections = mutableListOf<SectionModel>()
    val node = TransformNode("Render Geometry", scene)
    node.setEnabled(false)

    for (section in renderObject.sections) {
        val builder = VertexDataBuilder()

        for (obj in section.objects) {
            ninjaObjectToVertexDataBuilder(obj, builder)
        }

        val vertexData = builder.build()
        val mesh = Mesh("Render Geometry", scene, node)
        vertexData.applyToMesh(mesh)
        // TODO: Material.

        mesh.position = vec3ToBabylon(section.position)
        mesh.rotation = vec3ToBabylon(section.rotation)

        if (section.id >= 0) {
            val sec = SectionModel(
                section.id,
                vec3ToBabylon(section.position),
                vec3ToBabylon(section.rotation),
                areaVariant,
            )
            sections.add(sec)
            mesh.metadata = AreaMetadata(sec)
        }
    }

    return Pair(node, sections)
}

private fun areaCollisionGeometryToTransformNode(
    scene: Scene,
    obj: CollisionObject,
): TransformNode {
    val node = TransformNode("Collision Geometry", scene)

    for (collisionMesh in obj.meshes) {
        val builder = VertexDataBuilder()

        for (triangle in collisionMesh.triangles) {
            val isSectionTransition = (triangle.flags and 0b1000000) != 0
            val isVegetation = (triangle.flags and 0b10000) != 0
            val isGround = (triangle.flags and 0b1) != 0
            val colorIndex = when {
                isSectionTransition -> 3
                isVegetation -> 2
                isGround -> 1
                else -> 0
            }

            // Filter out walls.
            if (colorIndex != 0) {
                val p1 = vec3ToBabylon(collisionMesh.vertices[triangle.index1])
                val p2 = vec3ToBabylon(collisionMesh.vertices[triangle.index2])
                val p3 = vec3ToBabylon(collisionMesh.vertices[triangle.index3])
                val n = vec3ToBabylon(triangle.normal)

                builder.addIndex(builder.vertexCount)
                builder.addVertex(p1, n)
                builder.addIndex(builder.vertexCount)
                builder.addVertex(p3, n)
                builder.addIndex(builder.vertexCount)
                builder.addVertex(p2, n)
            }
        }

        if (builder.vertexCount > 0) {
            val mesh = Mesh("Collision Geometry", scene, parent = node)
            builder.build().applyToMesh(mesh)
        }
    }

    return node
}
