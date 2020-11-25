package world.phantasmal.web.questEditor.loading

import kotlinx.coroutines.CoroutineScope
import org.khronos.webgl.ArrayBuffer
import world.phantasmal.lib.Endianness
import world.phantasmal.lib.cursor.cursor
import world.phantasmal.lib.fileFormats.CollisionObject
import world.phantasmal.lib.fileFormats.RenderObject
import world.phantasmal.lib.fileFormats.parseAreaCollisionGeometry
import world.phantasmal.lib.fileFormats.parseAreaGeometry
import world.phantasmal.lib.fileFormats.quest.Episode
import world.phantasmal.web.core.euler
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.rendering.conversion.MeshBuilder
import world.phantasmal.web.core.rendering.conversion.ninjaObjectToMeshBuilder
import world.phantasmal.web.core.rendering.conversion.vec3ToThree
import world.phantasmal.web.core.rendering.disposeObject3DResources
import world.phantasmal.web.externals.three.*
import world.phantasmal.web.questEditor.models.AreaVariantModel
import world.phantasmal.web.questEditor.models.SectionModel
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.obj

/**
 * Loads and caches area assets.
 */
class AreaAssetLoader(
    scope: CoroutineScope,
    private val assetLoader: AssetLoader,
) : DisposableContainer() {
    /**
     * This cache's values consist of an Object3D containing the area render meshes and a list of
     * that area's sections.
     */
    private val renderObjectCache = addDisposable(
        LoadingCache<EpisodeAndAreaVariant, Pair<Object3D, List<SectionModel>>>(
            scope,
            { (episode, areaVariant) ->
                val buffer = getAreaAsset(episode, areaVariant, AssetType.Render)
                val obj = parseAreaGeometry(buffer.cursor(Endianness.Little))
                areaGeometryToTransformNodeAndSections(obj, areaVariant)
            },
            { (obj3d) -> disposeObject3DResources(obj3d) },
        )
    )

    private val collisionObjectCache = addDisposable(
        LoadingCache<EpisodeAndAreaVariant, Object3D>(
            scope,
            { (episode, areaVariant) ->
                val buffer = getAreaAsset(episode, areaVariant, AssetType.Collision)
                val obj = parseAreaCollisionGeometry(buffer.cursor(Endianness.Little))
                areaCollisionGeometryToTransformNode(obj, episode, areaVariant)
            },
            ::disposeObject3DResources,
        )
    )

    suspend fun loadSections(episode: Episode, areaVariant: AreaVariantModel): List<SectionModel> =
        loadRenderGeometryAndSections(episode, areaVariant).second

    suspend fun loadRenderGeometry(episode: Episode, areaVariant: AreaVariantModel): Object3D =
        loadRenderGeometryAndSections(episode, areaVariant).first

    private suspend fun loadRenderGeometryAndSections(
        episode: Episode,
        areaVariant: AreaVariantModel,
    ): Pair<Object3D, List<SectionModel>> =
        renderObjectCache.get(EpisodeAndAreaVariant(episode, areaVariant))

    suspend fun loadCollisionGeometry(
        episode: Episode,
        areaVariant: AreaVariantModel,
    ): Object3D =
        collisionObjectCache.get(EpisodeAndAreaVariant(episode, areaVariant))

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

    private data class EpisodeAndAreaVariant(
        val episode: Episode,
        val areaVariant: AreaVariantModel,
    )

    private enum class AssetType {
        Render, Collision
    }
}

interface AreaUserData {
    var sectionId: Int?
}

private val COLLISION_MATERIALS: Array<Material> = arrayOf(
    // Wall
    MeshBasicMaterial(obj {
        color = Color(0x80c0d0)
        transparent = true
        opacity = 0.25
    }),
    // Ground
    MeshLambertMaterial(obj {
        color = Color(0x405050)
        side = DoubleSide
    }),
    // Vegetation
    MeshLambertMaterial(obj {
        color = Color(0x306040)
        side = DoubleSide
    }),
    // Section transition zone
    MeshLambertMaterial(obj {
        color = Color(0x402050)
        side = DoubleSide
    }),
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
    renderObject: RenderObject,
    areaVariant: AreaVariantModel,
): Pair<Object3D, List<SectionModel>> {
    val sections = mutableListOf<SectionModel>()
    val obj3d = Group()

    for (section in renderObject.sections) {
        val builder = MeshBuilder()

        for (obj in section.objects) {
            ninjaObjectToMeshBuilder(obj, builder)
        }

        val mesh = builder.buildMesh()
        // TODO: Material.

        mesh.position.set(
            section.position.x.toDouble(),
            section.position.y.toDouble(),
            section.position.z.toDouble()
        )
        mesh.rotation.set(
            section.rotation.x.toDouble(),
            section.rotation.y.toDouble(),
            section.rotation.z.toDouble(),
        )
        mesh.updateMatrixWorld()

        if (section.id >= 0) {
            val sec = SectionModel(
                section.id,
                vec3ToThree(section.position),
                euler(section.rotation.x, section.rotation.y, section.rotation.z),
                areaVariant,
            )
            sections.add(sec)
        }

        (mesh.userData.unsafeCast<AreaUserData>()).sectionId = section.id.takeIf { it >= 0 }
        obj3d.add(mesh)
    }

    return Pair(obj3d, sections)
}

private fun areaCollisionGeometryToTransformNode(
    obj: CollisionObject,
    episode: Episode,
    areaVariant: AreaVariantModel,
): Object3D {
    val obj3d = Group()
    obj3d.name = "Collision Geometry $episode-${areaVariant.area.id}-${areaVariant.id}"

    for (collisionMesh in obj.meshes) {
        // Use Geometry instead of BufferGeometry for better raycaster performance.
        val geom = Geometry()

        geom.vertices = Array(collisionMesh.vertices.size) {
            vec3ToThree(collisionMesh.vertices[it])
        }

        for (triangle in collisionMesh.triangles) {
            val isSectionTransition = (triangle.flags and 0b1000000) != 0
            val isVegetation = (triangle.flags and 0b10000) != 0
            val isGround = (triangle.flags and 0b1) != 0
            val materialIndex = when {
                isSectionTransition -> 3
                isVegetation -> 2
                isGround -> 1
                else -> 0
            }

            // Filter out walls.
            if (materialIndex != 0) {
                geom.faces.asDynamic().push(
                    Face3(
                        triangle.index1,
                        triangle.index2,
                        triangle.index3,
                        vec3ToThree(triangle.normal),
                        materialIndex = materialIndex,
                    )
                )
            }
        }

        if (geom.faces.isNotEmpty()) {
            geom.computeBoundingBox()
            geom.computeBoundingSphere()
            obj3d.add(Mesh(geom, COLLISION_MATERIALS))
        }
    }

    return obj3d
}
