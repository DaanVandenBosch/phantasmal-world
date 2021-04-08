package world.phantasmal.web.questEditor.loading

import org.khronos.webgl.ArrayBuffer
import world.phantasmal.core.asJsArray
import world.phantasmal.core.isBitSet
import world.phantasmal.lib.Endianness
import world.phantasmal.lib.Episode
import world.phantasmal.lib.cursor.cursor
import world.phantasmal.lib.fileFormats.CollisionGeometry
import world.phantasmal.lib.fileFormats.RenderGeometry
import world.phantasmal.lib.fileFormats.ninja.XjObject
import world.phantasmal.lib.fileFormats.ninja.XvrTexture
import world.phantasmal.lib.fileFormats.ninja.parseXvm
import world.phantasmal.lib.fileFormats.parseAreaCollisionGeometry
import world.phantasmal.lib.fileFormats.parseAreaRenderGeometry
import world.phantasmal.web.core.dot
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.rendering.conversion.*
import world.phantasmal.web.core.rendering.disposeObject3DResources
import world.phantasmal.web.externals.three.*
import world.phantasmal.web.questEditor.models.AreaVariantModel
import world.phantasmal.web.questEditor.models.SectionModel
import world.phantasmal.webui.DisposableContainer
import kotlin.math.PI
import kotlin.math.cos

interface AreaUserData {
    var section: SectionModel?
}

/**
 * Loads and caches area assets.
 */
class AreaAssetLoader(private val assetLoader: AssetLoader) : DisposableContainer() {
    private val cache = addDisposable(
        LoadingCache<EpisodeAndAreaVariant, Geom>(
            { (episode, areaVariant) ->
                val renderObj = parseAreaRenderGeometry(
                    getAreaAsset(episode, areaVariant, AssetType.Render).cursor(Endianness.Little),
                )
                val xvm = parseXvm(
                    getAreaAsset(episode, areaVariant, AssetType.Texture).cursor(Endianness.Little),
                ).unwrap()
                val (renderObj3d, sections) = areaGeometryToObject3DAndSections(
                    renderObj,
                    xvm.textures,
                    episode,
                    areaVariant,
                )

                val collisionObj = parseAreaCollisionGeometry(
                    getAreaAsset(episode, areaVariant, AssetType.Collision)
                        .cursor(Endianness.Little)
                )
                val collisionObj3d =
                    areaCollisionGeometryToObject3D(collisionObj, episode, areaVariant)

                addSectionsToCollisionGeometry(collisionObj3d, renderObj3d)

//                cullRenderGeometry(collisionObj3d, renderObj3d)

                Geom(sections, renderObj3d, collisionObj3d)
            },
            { geom ->
                disposeObject3DResources(geom.renderGeometry)
                disposeObject3DResources(geom.collisionGeometry)
            },
        )
    )

    suspend fun loadSections(episode: Episode, areaVariant: AreaVariantModel): List<SectionModel> =
        cache.get(EpisodeAndAreaVariant(episode, areaVariant)).sections

    suspend fun loadRenderGeometry(episode: Episode, areaVariant: AreaVariantModel): Object3D =
        cache.get(EpisodeAndAreaVariant(episode, areaVariant)).renderGeometry

    suspend fun loadCollisionGeometry(
        episode: Episode,
        areaVariant: AreaVariantModel,
    ): Object3D =
        cache.get(EpisodeAndAreaVariant(episode, areaVariant)).collisionGeometry

    private suspend fun getAreaAsset(
        episode: Episode,
        areaVariant: AreaVariantModel,
        type: AssetType,
    ): ArrayBuffer {
        return assetLoader.loadArrayBuffer(areaAssetUrl(episode, areaVariant, type))
    }

    private fun addSectionsToCollisionGeometry(collisionGeom: Object3D, renderGeom: Object3D) {
        for (collisionMesh in collisionGeom.children) {
            val origin = ((collisionMesh as Mesh).geometry).boundingBox!!.getCenter(tmpVec)

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
                val cud = collisionMesh.userData.unsafeCast<AreaUserData>()
                val rud = intersection.`object`.userData.unsafeCast<AreaUserData>()
                cud.section = rud.section
            }
        }
    }

    private fun cullRenderGeometry(collisionGeom: Object3D, renderGeom: Object3D) {
        val cullingVolumes = mutableMapOf<Int, Box3>()

        for (collisionMesh in collisionGeom.children) {
            collisionMesh as Mesh
            collisionMesh.userData.unsafeCast<AreaUserData>().section?.let { section ->
                cullingVolumes.getOrPut(section.id, ::Box3)
                    .union(
                        collisionMesh.geometry.boundingBox!!.applyMatrix4(collisionMesh.matrixWorld)
                    )
            }
        }

        for (cullingVolume in cullingVolumes.values) {
            cullingVolume.min.x -= 50
            cullingVolume.min.y = cullingVolume.max.y + 20
            cullingVolume.min.z -= 50
            cullingVolume.max.x += 50
            cullingVolume.max.y = Double.POSITIVE_INFINITY
            cullingVolume.max.z += 50
        }

        var i = 0

        outer@ while (i < renderGeom.children.size) {
            val renderMesh = renderGeom.children[i] as Mesh
            val bb = renderMesh.geometry.boundingBox!!.applyMatrix4(renderMesh.matrixWorld)

            for (cullingVolume in cullingVolumes.values) {
                if (bb.intersectsBox(cullingVolume)) {
                    renderGeom.remove(renderMesh)
                    continue@outer
                }
            }

            i++
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
        renderGeometry: RenderGeometry,
        textures: List<XvrTexture>,
        episode: Episode,
        areaVariant: AreaVariantModel,
    ): Pair<Object3D, List<SectionModel>> {
        val sections = mutableMapOf<Int, SectionModel>()

        val group =
            renderGeometryToGroup(renderGeometry, textures) { renderSection, xjObject, mesh ->
                if (shouldRenderOnTop(xjObject, episode, areaVariant)) {
                    mesh.renderOrder = 1
                }

                if (renderSection.id >= 0) {
                    val sectionModel = sections.getOrPut(renderSection.id) {
                        SectionModel(
                            renderSection.id,
                            vec3ToThree(renderSection.position),
                            vec3ToEuler(renderSection.rotation),
                            areaVariant,
                        )
                    }

                    (mesh.userData.unsafeCast<AreaUserData>()).section = sectionModel
                }
            }

        return Pair(group, sections.values.toList())
    }

    private fun shouldRenderOnTop(
        obj: XjObject,
        episode: Episode,
        areaVariant: AreaVariantModel,
    ): Boolean {
        fun recurse(obj: XjObject): Boolean {
            obj.model?.meshes?.let { meshes ->
                for (mesh in meshes) {
                    mesh.material.textureId?.let { textureId ->
                        RENDER_ON_TOP_TEXTURES[Pair(episode, areaVariant.id)]?.let { textureIds ->
                            if (textureId in textureIds) {
                                return true
                            }
                        }
                    }
                }
            }

            return obj.children.any(::recurse)
        }

        return recurse(obj)
    }

    private fun areaCollisionGeometryToObject3D(
        obj: CollisionGeometry,
        episode: Episode,
        areaVariant: AreaVariantModel,
    ): Object3D {
        val group = collisionGeometryToGroup(obj) {
            // Filter out walls and steep triangles.
            if (it.flags.isBitSet(0) || it.flags.isBitSet(4) || it.flags.isBitSet(6)) {
                tmpVec.setFromVec3(it.normal)
                tmpVec dot UP >= COS_75_DEG
            } else {
                false
            }
        }
        group.name = "Collision Geometry $episode-${areaVariant.area.id}-${areaVariant.id}"
        return group
    }

    private data class EpisodeAndAreaVariant(
        val episode: Episode,
        val areaVariant: AreaVariantModel,
    )

    private class Geom(
        val sections: List<SectionModel>,
        val renderGeometry: Object3D,
        val collisionGeometry: Object3D,
    )

    private enum class AssetType {
        Render, Collision, Texture
    }

    companion object {
        private val COS_75_DEG = cos(PI / 180 * 75)
        private val DOWN = Vector3(.0, -1.0, .0)
        private val UP = Vector3(.0, 1.0, .0)

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

        /**
         * Mapping of episode and area ID to set of texture IDs.
         * Manual fixes for various areas. Might not be necessary anymore once order-independent
         * rendering is implemented.
         */
        val RENDER_ON_TOP_TEXTURES: Map<Pair<Episode, Int>, Set<Int>> = mapOf(
            // Pioneer 2
            Pair(Episode.I, 0) to setOf(
                70, 71, 72, 126, 127, 155, 156, 198, 230, 231, 232, 233, 234,
            ),
            // Forest 1
            Pair(Episode.I, 1) to setOf(12, 41),
            // Mine 2
            Pair(Episode.I, 7) to setOf(0, 1, 7, 8, 17, 23, 56, 57, 58, 59, 60, 83),
            // Ruins 1
            Pair(Episode.I, 8) to setOf(1, 21, 22, 27, 28, 43, 51, 59, 70, 72, 75),
            // Lab
            Pair(Episode.II, 0) to setOf(36, 37, 38, 48, 60, 67, 79, 80),
            // Central Control Area
            Pair(Episode.II, 5) to (0..59).toSet() + setOf(69, 77),
        )

        private val raycaster = Raycaster()
        private val tmpVec = Vector3()
        private val tmpIntersections = arrayOf<Intersection>()
    }
}
