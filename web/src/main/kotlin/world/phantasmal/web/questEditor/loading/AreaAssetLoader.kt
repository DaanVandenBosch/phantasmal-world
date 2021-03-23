package world.phantasmal.web.questEditor.loading

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Float32Array
import org.khronos.webgl.Uint16Array
import world.phantasmal.core.JsArray
import world.phantasmal.core.asArray
import world.phantasmal.core.asJsArray
import world.phantasmal.core.jsArrayOf
import world.phantasmal.lib.Endianness
import world.phantasmal.lib.Episode
import world.phantasmal.lib.cursor.cursor
import world.phantasmal.lib.fileFormats.CollisionObject
import world.phantasmal.lib.fileFormats.RenderObject
import world.phantasmal.lib.fileFormats.parseAreaCollisionGeometry
import world.phantasmal.lib.fileFormats.parseAreaGeometry
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.rendering.conversion.*
import world.phantasmal.web.core.rendering.disposeObject3DResources
import world.phantasmal.web.externals.three.*
import world.phantasmal.web.questEditor.models.AreaVariantModel
import world.phantasmal.web.questEditor.models.SectionModel
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.obj

/**
 * Loads and caches area assets.
 */
class AreaAssetLoader(private val assetLoader: AssetLoader) : DisposableContainer() {
    /**
     * This cache's values consist of an Object3D containing the area render meshes and a list of
     * that area's sections.
     */
    private val renderObjectCache = addDisposable(
        LoadingCache<EpisodeAndAreaVariant, Pair<Object3D, List<SectionModel>>>(
            { (episode, areaVariant) ->
                val buffer = getAreaAsset(episode, areaVariant, AssetType.Render)
                val obj = parseAreaGeometry(buffer.cursor(Endianness.Little))
                areaGeometryToObject3DAndSections(obj, areaVariant)
            },
            { (obj3d) -> disposeObject3DResources(obj3d) },
        )
    )

    private val collisionObjectCache = addDisposable(
        LoadingCache<EpisodeAndAreaVariant, Object3D>(
            { key ->
                val (episode, areaVariant) = key
                val buffer = getAreaAsset(episode, areaVariant, AssetType.Collision)
                val obj = parseAreaCollisionGeometry(buffer.cursor(Endianness.Little))
                val obj3d = areaCollisionGeometryToObject3D(obj, episode, areaVariant)

                val (renderObj3d) = renderObjectCache.get(key)
                addSectionsToCollisionGeometry(obj3d, renderObj3d)

                obj3d
            },
            ::disposeObject3DResources,
        )
    )

    suspend fun loadSections(episode: Episode, areaVariant: AreaVariantModel): List<SectionModel> =
        renderObjectCache.get(EpisodeAndAreaVariant(episode, areaVariant)).second

    suspend fun loadRenderGeometry(episode: Episode, areaVariant: AreaVariantModel): Object3D =
        renderObjectCache.get(EpisodeAndAreaVariant(episode, areaVariant)).first

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

    private fun addSectionsToCollisionGeometry(collisionGeom: Object3D, renderGeom: Object3D) {
        for (collisionArea in collisionGeom.children) {
            val origin = ((collisionArea as Mesh).geometry).boundingBox!!.getCenter(tmpVec)

            // Cast a ray downward from the center of the section.
            raycaster.set(origin, DOWN)
            tmpIntersections.asJsArray().splice(0)
            val intersection1 = raycaster
                .intersectObject(renderGeom, true, tmpIntersections)
                .find { (it.`object`.userData.unsafeCast<AreaUserData>()).section != null }

            // Cast a ray upward from the center of the section.
            raycaster.set(origin, UP)
            tmpIntersections.asJsArray().splice(0)
            val intersection2 = raycaster
                .intersectObject(renderGeom, true, tmpIntersections)
                .find { (it.`object`.userData.unsafeCast<AreaUserData>()).section != null }

            // Choose the nearest intersection if we have 2.
            val intersection =
                if (intersection1 != null && intersection2 != null) {
                    if (intersection1.distance <= intersection2.distance) intersection1
                    else intersection2
                } else {
                    intersection1 ?: intersection2
                }

            if (intersection != null) {
                val cud = collisionArea.userData.unsafeCast<AreaUserData>()
                val rud = intersection.`object`.userData.unsafeCast<AreaUserData>()
                cud.section = rud.section
            }
        }
    }

    private data class EpisodeAndAreaVariant(
        val episode: Episode,
        val areaVariant: AreaVariantModel,
    )

    private enum class AssetType {
        Render, Collision
    }

    companion object {
        private val DOWN = Vector3(.0, -1.0, .0)
        private val UP = Vector3(.0, 1.0, .0)
        private val raycaster = Raycaster()
        private val tmpVec = Vector3()
        private val tmpIntersections = arrayOf<Intersection>()
    }
}

interface AreaUserData {
    var section: SectionModel?
}

private val COLLISION_MATERIALS: Array<Material> = arrayOf(
    // Wall
    MeshBasicMaterial(obj {
        color = Color(0x80c0d0)
        transparent = true
        opacity = .25
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

private val COLLISION_WIREFRAME_MATERIALS: Array<Material> = arrayOf(
    // Wall
    MeshBasicMaterial(obj {
        color = Color(0x90d0e0)
        wireframe = true
        transparent = true
        opacity = .3
    }),
    // Ground
    MeshBasicMaterial(obj {
        color = Color(0x506060)
        wireframe = true
    }),
    // Vegetation
    MeshBasicMaterial(obj {
        color = Color(0x405050)
        wireframe = true
    }),
    // Section transition zone
    MeshBasicMaterial(obj {
        color = Color(0x503060)
        wireframe = true
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

    // Exception for Seaside Area at Night, variant 1.
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

private fun areaGeometryToObject3DAndSections(
    renderObject: RenderObject,
    areaVariant: AreaVariantModel,
): Pair<Object3D, List<SectionModel>> {
    val sections = mutableListOf<SectionModel>()
    val obj3d = Group()

    for ((i, section) in renderObject.sections.withIndex()) {
        val builder = MeshBuilder()

        for (obj in section.objects) {
            ninjaObjectToMeshBuilder(obj, builder)
        }

        builder.defaultMaterial(MeshBasicMaterial(obj {
            color = Color().setHSL((i % 7) / 7.0, 1.0, .5)
            transparent = true
            opacity = .25
            side = DoubleSide
        }))

        val mesh = builder.buildMesh()

        mesh.position.setFromVec3(section.position)
        mesh.rotation.setFromVec3(section.rotation)
        mesh.updateMatrixWorld()

        if (section.id >= 0) {
            val sectionModel = SectionModel(
                section.id,
                vec3ToThree(section.position),
                vec3ToEuler(section.rotation),
                areaVariant,
            )
            sections.add(sectionModel)
            (mesh.userData.unsafeCast<AreaUserData>()).section = sectionModel
        }

        obj3d.add(mesh)
    }

    return Pair(obj3d, sections)
}

private fun areaCollisionGeometryToObject3D(
    obj: CollisionObject,
    episode: Episode,
    areaVariant: AreaVariantModel,
): Object3D {
    val group = Group()
    group.name = "Collision Geometry $episode-${areaVariant.area.id}-${areaVariant.id}"

    for (collisionMesh in obj.meshes) {
        val positions = jsArrayOf<Float>()
        val normals = jsArrayOf<Float>()
        val materialGroups = mutableMapOf<Int, JsArray<Short>>()
        var index: Short = 0

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
                val p1 = collisionMesh.vertices[triangle.index1]
                val p2 = collisionMesh.vertices[triangle.index2]
                val p3 = collisionMesh.vertices[triangle.index3]
                positions.push(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, p3.x, p3.y, p3.z)

                val n = triangle.normal
                normals.push(n.x, n.y, n.z, n.x, n.y, n.z, n.x, n.y, n.z)

                val indices = materialGroups.getOrPut(materialIndex) { jsArrayOf() }
                indices.push(index++, index++, index++)
            }
        }

        if (index > 0) {
            val geom = BufferGeometry()
            geom.setAttribute(
                "position", Float32BufferAttribute(Float32Array(positions.asArray()), 3),
            )
            geom.setAttribute(
                "normal", Float32BufferAttribute(Float32Array(normals.asArray()), 3),
            )
            val indices = Uint16Array(index.toInt())
            var offset = 0

            for ((materialIndex, vertexIndices) in materialGroups) {
                indices.set(vertexIndices.asArray(), offset)
                geom.addGroup(offset, vertexIndices.length, materialIndex)
                offset += vertexIndices.length
            }

            geom.setIndex(Uint16BufferAttribute(indices, 1))
            geom.computeBoundingBox()
            geom.computeBoundingSphere()

            group.add(
                Mesh(geom, COLLISION_MATERIALS).apply {
                    renderOrder = 1
                }
            )

            group.add(
                Mesh(geom, COLLISION_WIREFRAME_MATERIALS).apply {
                    renderOrder = 2
                }
            )
        }
    }

    return group
}
