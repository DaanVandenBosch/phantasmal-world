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
import world.phantasmal.lib.fileFormats.ninja.NinjaObject
import world.phantasmal.lib.fileFormats.ninja.XjModel
import world.phantasmal.lib.fileFormats.ninja.XvrTexture
import world.phantasmal.lib.fileFormats.ninja.parseXvm
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

interface AreaUserData {
    var section: SectionModel?
}

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
                val obj = parseAreaGeometry(
                    getAreaAsset(episode, areaVariant, AssetType.Render).cursor(Endianness.Little),
                )
                val xvm = parseXvm(
                    getAreaAsset(episode, areaVariant, AssetType.Texture).cursor(Endianness.Little),
                ).unwrap()
                areaGeometryToObject3DAndSections(obj, xvm.textures, episode, areaVariant)
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
        return assetLoader.loadArrayBuffer(areaAssetUrl(episode, areaVariant, type))
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

    private fun areaAssetUrl(
        episode: Episode,
        areaVariant: AreaVariantModel,
        type: AssetType,
    ): String {
        var areaId = areaVariant.area.id
        var areaVariantId = areaVariant.id

        // Exception for Seaside Area at Night, variant 1.
        // Phantasmal World 4 and Lost heart breaker use this to have two tower maps.
        if (episode == Episode.II && areaId == 16 && areaVariantId == 1) {
            areaId = 17
            areaVariantId = 1
        }

        // Exception for Crater Route 1-4, naming is slightly different.
        if (episode == Episode.IV && areaId in 1..4) {
            areaVariantId = areaId - 1
        }

        val episodeBaseNames = AREA_BASE_NAMES.getValue(episode)

        require(areaId in episodeBaseNames.indices) {
            "Unknown episode $episode area $areaId."
        }

        val (baseName, addVariant) = episodeBaseNames[areaId]

        val variant = if (addVariant && type != AssetType.Texture) {
            "_" + areaVariantId.toString().padStart(2, '0')
        } else {
            ""
        }

        val suffix = when (type) {
            AssetType.Render -> "n.rel"
            AssetType.Collision -> "c.rel"
            AssetType.Texture -> ".xvm"
        }

        return "/areas/map_${baseName}${variant}${suffix}"
    }

    private fun areaGeometryToObject3DAndSections(
        renderObject: RenderObject,
        textures: List<XvrTexture>,
        episode: Episode,
        areaVariant: AreaVariantModel,
    ): Pair<Object3D, List<SectionModel>> {
        val sections = mutableListOf<SectionModel>()
        val group = Group()
        val textureCache = mutableMapOf<Int, Texture?>()

        for ((i, section) in renderObject.sections.withIndex()) {
            val sectionModel = if (section.id >= 0) {
                SectionModel(
                    section.id,
                    vec3ToThree(section.position),
                    vec3ToEuler(section.rotation),
                    areaVariant,
                ).also(sections::add)
            } else null

            for (obj in section.objects) {
                val builder = MeshBuilder(textures, textureCache)
                ninjaObjectToMeshBuilder(obj, builder)

                builder.defaultMaterial(MeshBasicMaterial(obj {
                    color = Color().setHSL((i % 7) / 7.0, 1.0, .5)
                    transparent = true
                    opacity = .25
                    side = DoubleSide
                }))

                val mesh = builder.buildMesh()

                if (shouldRenderOnTop(obj, episode, areaVariant)) {
                    mesh.renderOrder = 1
                }

                mesh.position.setFromVec3(section.position)
                mesh.rotation.setFromVec3(section.rotation)
                mesh.updateMatrixWorld()

                sectionModel?.let {
                    (mesh.userData.unsafeCast<AreaUserData>()).section = sectionModel
                }

                group.add(mesh)
            }
        }

        return Pair(group, sections)
    }

    private fun shouldRenderOnTop(
        obj: NinjaObject<XjModel>,
        episode: Episode,
        areaVariant: AreaVariantModel,
    ): Boolean {
        // Manual fixes for various areas. Might not be necessary anymore once order-independent
        // rendering is implemented.
        val textureIds: Set<Int> = when {
            // Pioneer 2
            episode == Episode.I && areaVariant.area.id == 0 ->
                setOf(70, 71, 72, 126, 127, 155, 156, 198, 230, 231, 232, 233, 234)
            // Forest 1
            episode == Episode.I && areaVariant.area.id == 1 ->
                setOf(12, 41)
            // Mine 2
            episode == Episode.I && areaVariant.area.id == 7 ->
                setOf(0, 1, 7, 8, 17, 23, 56, 57, 58, 59, 60, 83)
            // Ruins 1
            episode == Episode.I && areaVariant.area.id == 8 ->
                setOf(1, 21, 22, 27, 28, 43, 51, 59, 70, 72, 75)
            // Lab
            episode == Episode.II && areaVariant.area.id == 0 ->
                setOf(36, 37, 38, 48, 60, 67, 79, 80)
            // Central Control Area
            episode == Episode.II && areaVariant.area.id == 5 ->
                (0..59).toSet() + setOf(69, 77)
            else ->
                return false
        }

        fun recurse(obj: NinjaObject<XjModel>): Boolean {
            obj.model?.meshes?.let { meshes ->
                for (mesh in meshes) {
                    mesh.material.textureId?.let {
                        if (it in textureIds) {
                            return true
                        }
                    }
                }
            }

            return obj.children.any(::recurse)
        }

        return recurse(obj)
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

    private data class EpisodeAndAreaVariant(
        val episode: Episode,
        val areaVariant: AreaVariantModel,
    )

    private enum class AssetType {
        Render, Collision, Texture
    }

    companion object {
        private val DOWN = Vector3(.0, -1.0, .0)
        private val UP = Vector3(.0, 1.0, .0)

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

        private val AREA_BASE_NAMES: Map<Episode, List<Pair<String, Boolean>>> = mapOf(
            Episode.I to listOf(
                Pair("city00", true),
                Pair("forest01", false),
                Pair("forest02", false),
                Pair("cave01", true),
                Pair("cave02", true),
                Pair("cave03", true),
                Pair("machine01", true),
                Pair("machine02", true),
                Pair("ancient01", true),
                Pair("ancient02", true),
                Pair("ancient03", true),
                Pair("boss01", false),
                Pair("boss02", false),
                Pair("boss03", false),
                Pair("darkfalz00", false),
            ),
            Episode.II to listOf(
                Pair("labo00", true),
                Pair("ruins01", true),
                Pair("ruins02", true),
                Pair("space01", true),
                Pair("space02", true),
                Pair("jungle01", true),
                Pair("jungle02", true),
                Pair("jungle03", true),
                Pair("jungle04", true),
                Pair("jungle05", true),
                Pair("seabed01", true),
                Pair("seabed02", true),
                Pair("boss05", false),
                Pair("boss06", false),
                Pair("boss07", false),
                Pair("boss08", false),
                Pair("jungle06", true),
                Pair("jungle07", true),
            ),
            Episode.IV to listOf(
                Pair("city02", true),
                Pair("wilds01", true),
                Pair("wilds01", true),
                Pair("wilds01", true),
                Pair("wilds01", true),
                Pair("crater01", true),
                Pair("desert01", true),
                Pair("desert02", true),
                Pair("desert03", true),
                Pair("boss09", true),
            )
        )

        private val raycaster = Raycaster()
        private val tmpVec = Vector3()
        private val tmpIntersections = arrayOf<Intersection>()
    }
}
