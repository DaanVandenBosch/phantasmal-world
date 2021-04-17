package world.phantasmal.web.questEditor.loading

import org.khronos.webgl.ArrayBuffer
import world.phantasmal.core.*
import world.phantasmal.lib.Endianness
import world.phantasmal.lib.Episode
import world.phantasmal.lib.cursor.cursor
import world.phantasmal.lib.fileFormats.*
import world.phantasmal.lib.fileFormats.ninja.XjObject
import world.phantasmal.lib.fileFormats.ninja.XvrTexture
import world.phantasmal.lib.fileFormats.ninja.parseXvm
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

    fun getCachedSections(episode: Episode, areaVariant: AreaVariantModel): List<SectionModel>? =
        cache.getIfPresentNow(EpisodeAndAreaVariant(episode, areaVariant))?.sections

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

//    private fun cullRenderGeometry(collisionGeom: Object3D, renderGeom: Object3D) {
//        val cullingVolumes = mutableMapOf<Int, Box3>()
//
//        for (collisionMesh in collisionGeom.children) {
//            collisionMesh as Mesh
//            collisionMesh.userData.unsafeCast<AreaUserData>().section?.let { section ->
//                cullingVolumes.getOrPut(section.id, ::Box3)
//                    .union(
//                        collisionMesh.geometry.boundingBox!!.applyMatrix4(collisionMesh.matrixWorld)
//                    )
//            }
//        }
//
//        for (cullingVolume in cullingVolumes.values) {
//            cullingVolume.min.x -= 50
//            cullingVolume.min.y = cullingVolume.max.y + 20
//            cullingVolume.min.z -= 50
//            cullingVolume.max.x += 50
//            cullingVolume.max.y = Double.POSITIVE_INFINITY
//            cullingVolume.max.z += 50
//        }
//
//        var i = 0
//
//        outer@ while (i < renderGeom.children.size) {
//            val renderMesh = renderGeom.children[i] as Mesh
//            val bb = renderMesh.geometry.boundingBox!!.applyMatrix4(renderMesh.matrixWorld)
//
//            for (cullingVolume in cullingVolumes.values) {
//                if (bb.intersectsBox(cullingVolume)) {
//                    renderGeom.remove(renderMesh)
//                    continue@outer
//                }
//            }
//
//            i++
//        }
//    }

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
        renderGeometry: AreaGeometry,
        textures: List<XvrTexture>,
        episode: Episode,
        areaVariant: AreaVariantModel,
    ): Pair<Object3D, List<SectionModel>> {
        val fix = MANUAL_FIXES[Pair(episode, areaVariant.area.id)]
        val sections = mutableMapOf<Int, SectionModel>()

        val group =
            renderGeometryToGroup(renderGeometry, textures) { renderSection, areaObj, mesh ->
                if (fix != null) {
                    if (fix.shouldRenderOnTop(areaObj.xjObject)) {
                        mesh.renderOrder = 1
                    }

                    if (fix.shouldHide(areaObj)) {
                        mesh.visible = false
                    }
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

    private class Fix(
        /**
         * Textures that should be rendered on top of other textures. These are usually very
         * translucent. E.g. forest 1 has a mesh with baked-in shadow that's overlaid over the
         * regular geometry. Might not be necessary anymore once order-independent rendering is
         * implemented.
         */
        private val renderOnTopTextures: JsSet<Int> = emptyJsSet(),
        /**
         * Set of [AreaObject] finger prints.
         * These objects should be hidden because they cover floors and other useful geometry.
         */
        private val hiddenObjects: JsSet<String> = emptyJsSet(),
    ) {
        fun shouldRenderOnTop(obj: XjObject): Boolean {
            obj.model?.meshes?.let { meshes ->
                for (mesh in meshes) {
                    mesh.material.textureId?.let { textureId ->
                        if (renderOnTopTextures.has(textureId)) {
                            return true
                        }
                    }
                }
            }

            return obj.children.any(::shouldRenderOnTop)
        }

        fun shouldHide(areaObj: AreaObject): Boolean =
            hiddenObjects.has(areaObj.fingerPrint())
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
         * Mapping of episode and area ID to data for manually fixing issues with the render
         * geometry.
         */
        private val MANUAL_FIXES: Map<Pair<Episode, Int>, Fix> = mutableMapOf(
            // Pioneer 2
            Pair(Episode.I, 0) to Fix(
                renderOnTopTextures = jsSetOf(
                    20,
                    40,
                    41,
                    67,
                    70,
                    71,
                    72,
                    93,
                    94,
                    96,
                    105,
                    120,
                    121,
                    122,
                    137,
                    155,
                    156,
                    170,
                    198,
                    218,
                    220,
                    221,
                    230,
                    231,
                    232,
                    233,
                    234,
                    243,
                ),
                hiddenObjects = jsSetOf(
                    "s_m_0_6a_d_iu5sg6",
                    "s_m_0_4b_7_ioh738",
                    "s_k_0_1s_3_irasis",
                    "s_k_0_a_1_ir4eod",
                    "s_n_0_9e_h_imjyqr", // Hunter Guild roof + walls (seems to remove slightly too much).
                    "s_n_0_40_a_it58n7", // Neon signs throughout the city.
                    "s_n_0_2m_1_isvawv",
                    "s_n_0_o_1_iwk2nr",
                    "a_n_0_2k_5_iyebd3",
                    "s_n_0_4_1_ikyjfd",
                    "s_n_0_g_1_iom8uk",
                    "s_n_0_j5_b_ivdcj1",
                    "s_n_0_28_1_iopx1k",
                    "s_m_0_3q_6_iqmvjr",
                    "s_m_0_26_2_inh1ma",
                    "s_m_0_4b_4_immz8l",
                    "s_m_0_22_2_ilwnn5",
                    "s_m_0_84_e_iv6noc",
                    "s_m_0_d_1_ili3v2",
                    "s_m_0_58_2_igd0am",
                    "s_m_0_25_3_iovf21",
                    "s_n_0_8_1_ik11uc",
                    "s_m_0_19_1_ijocvh",
                    "s_m_0_2h_5_is8o4b",
                    "s_m_0_1l_4_ilkky7",
                    "s_m_0_35_1_il8hoa",
                    "s_m_0_58_3_in4nwl",
                    "s_m_0_3d_1_iro50a",
                    "s_m_0_4_1_is53va",
                    "s_m_0_3l_6_igzvga",
                    "s_n_0_en_3_iiawrz",
                    "a_k_0_1k_3_ihdi9s",
                    "s_n_0_c_1_jbhy8b",
                ),
            ),
            // Forest 1
            Pair(Episode.I, 1) to Fix(
                renderOnTopTextures = jsSetOf(12, 24, 41),
                // Hiding trees and their shadows, doesn't seem to improve things.
//                hiddenObjects = jsSetOf(
//                    // Trees
//                    "s_n_0_2i_2_im1teq",
//                    "s_n_0_2i_2_im1tep",
//                    // Baked-in shadows.
//                    "s_n_0_2n_1_irv6ha",
//                    "s_n_0_2g_1_iqvdru",
//                    "s_n_0_2p_1_irv6h6",
//                    "s_n_0_47_1_iqve55",
//                    "s_n_0_3p_1_ip2uvk",
//                    "s_n_0_1r_1_iqve64",
//                    "s_n_0_1f_1_irv6h0",
//                    "s_n_0_1o_1_iqvdnv",
//                    "s_n_0_28_1_iqvdw5",
//                    "s_n_0_2b_1_iqvdrj",
//                    "s_n_0_22_1_ip2w4r",
//                    "s_n_0_3i_1_iqvdxp",
//                    "s_n_0_33_1_iqve4e",
//                    "s_n_0_2i_1_ip2v47",
//                    "s_n_0_3a_1_ip2ugm",
//                    "s_n_0_39_1_irv672",
//                ),
            ),
            // Cave 1
            Pair(Episode.I, 3) to Fix(
                renderOnTopTextures = jsSetOf(89),
                hiddenObjects = jsSetOf(
                    "s_n_0_8_1_iqrqjj",
                    "s_i_0_b5_1_is7ajh",
                    "s_n_0_24_1_in5ce2",
                    "s_n_0_u_3_im4944",
                    "s_n_0_1b_2_im4945",
                    "s_n_0_2b_1_iktmat",
                    "s_n_0_3c_1_iksavp",
                    "s_n_0_31_1_ijhyzw",
                    "s_n_0_2i_3_ik3g7o",
                    "s_n_0_39_1_ix3ls0",
                    "s_n_0_37_1_ix3nxi",
                    "s_n_0_8x_1_iw2lqw",
                    "s_n_0_8w_1_ivx9ro",
                    "s_n_0_2c_1_itkfue",
                    "s_n_0_2u_1_iuilbk",
                    "s_n_0_30_1_ivmffx",
                    "s_n_0_2o_1_iu42tg",
                    "s_n_0_1u_1_ipk1qq",
                    "s_n_0_3i_1_iuz9mq",
                    "s_n_0_36_1_itm5fi",
                    "s_n_0_2o_1_ircjgr",
                    "s_n_0_3i_1_iurb4o",
                    "s_n_0_22_1_ii9035",
                    "s_n_0_2i_3_iiqupy",
                    "s_n_0_s_3_im3sg2",
                    "s_n_0_o_2_im3v5x",
                    "s_n_0_52_2_ilqxdf",
                    "s_n_0_1g_3_im5sui",
                    "s_n_0_15_2_im5sum",
                    "s_n_0_6l_1_im1ktx",
                    "s_n_0_3v_1_ikzchf",
                    "s_n_0_2i_3_ilfw56",
                ),
            ),
            // Cave 2
            Pair(Episode.I, 4) to Fix(
                hiddenObjects = jsSetOf(
                    "s_n_0_4j_1_irf90i",
                    "s_n_0_5i_1_iqqrft",
                    "s_n_0_g_1_iipv9r",
                    "s_n_0_c_1_ihboen",
                    "s_n_0_3l_2_iljrhl",
                    "s_n_0_5t_2_ill0ej",
                    "s_n_0_4e_2_iobj4y", // Deletes useful walls.
                    "s_n_0_6y_2_ipln11", // Deletes useful walls.
                    "s_n_0_43_1_iqbzr4",
                    "s_n_0_o_1_ikqpac",
                    "s_n_0_c_1_ihrvdk",
                    "s_n_0_c_1_ih2ob6",
                    "s_n_0_c_1_ihwsxo",
                    "s_n_0_c_1_igrh47",
                    "s_n_0_j9_4_iqqrft", // Deletes useful walls.
                    "s_n_0_p_2_ihe7ca",
                    "s_n_0_l_2_igkyx3",
                    "s_n_0_n_2_igubtb",
                    "s_n_0_l_2_ihuczl",
                    "s_n_0_o_1_ijn9y2",
                    "s_n_0_f_1_ijpzol",
                    "s_n_0_2n_1_ilgim5",
                ),
            ),
            // Cave 3
            Pair(Episode.I, 5) to Fix(
                hiddenObjects = jsSetOf(
                    "s_n_0_2o_5_inun1c",
                    "s_n_0_5y_2_ipyair",
                ),
            ),
            // Mine 1
            Pair(Episode.I, 6) to Fix(
                hiddenObjects = jsSetOf(
                    "s_n_0_2e_2_iqfpg8",
                    "s_n_0_d_1_iruof6",
                    "s_n_0_o_1_im9ta5",
                    "s_n_0_18_3_im1kwg",
                ),
            ),
            // Mine 2
            Pair(Episode.I, 7) to Fix(
                renderOnTopTextures = jsSetOf(0, 1, 7, 8, 17, 23, 56, 57, 58, 59, 60, 83),
            ),
            // Ruins 1
            Pair(Episode.I, 8) to Fix(
                renderOnTopTextures = jsSetOf(1, 21, 22, 27, 28, 43, 51, 59, 70, 72, 75),
                hiddenObjects = jsSetOf(
                    "s_n_0_2p_4_iohs6r",
                    "s_n_0_2q_4_iohs6r",
                    "s_m_0_l_1_io448k",
                ),
            ),
            // Ruins 2
            Pair(Episode.I, 9) to Fix(
                hiddenObjects = jsSetOf(
                    "s_m_0_l_1_io448k",
                ),
            ),
            // Lab
            Pair(Episode.II, 0) to Fix(
                renderOnTopTextures = jsSetOf(36, 37, 38, 48, 60, 67, 79, 80),
            ),
            // VR Spaceship Alpha
            Pair(Episode.II, 3) to Fix(
                renderOnTopTextures = jsSetOf(7, 59),
                hiddenObjects = jsSetOf(
                    "s_l_0_45_5_ing07n",
                    "s_n_0_45_5_ing07k",
                    "s_n_0_g2_b_im2en1",
                    "s_n_0_3j_1_irr4qe",
                    "s_n_0_bp_8_irbqmy",
                    "s_n_0_4h_1_irkudv",
                    "s_n_0_4g_1_irkudv",
                    "s_n_0_l_1_ijtl6r",
                    "s_n_0_l_1_ijtl6u",
                    "s_n_0_1s_1_imgj8o",
                    "s_n_0_r_1_ijua1b",
                    "s_n_0_g0_c_ilpett",
                    "s_n_0_16_1_igxq22",
                    "s_n_0_1c_1_imgj8o",
                    "s_n_0_1c_1_imgj8p",
                    "s_n_0_1u_1_imgj8o",
                    "s_n_0_1u_1_imgj8p",
                    "s_n_0_20_1_im13wb",
                    "s_n_0_12_1_ilsbgy",
                    "s_n_0_8_1_ihmjxh",
                    "s_n_0_1u_1_imv5rn",
                    "s_i_0_2d_4_ir3kzk",
                    "s_g_0_2d_4_ir3kzk",
                    "s_n_0_1t_1_imgj8o",
                    "s_n_0_l_1_ijoqlv",
                    "s_m_0_c_1_iayi9w",
                    "s_k_0_c_1_iayi9w",
                    "s_n_0_gl_8_imtj35",
                    "s_n_0_gc_8_imtj35",
                    "s_n_0_g_1_ildjm9",
                ),
            ),
            // Central Control Area
            Pair(Episode.II, 5) to Fix(
                renderOnTopTextures = jsSetOf(*((0..59).toSet() + setOf(69, 77)).toTypedArray()),
            ),
            // Jungle Area East
            Pair(Episode.II, 6) to Fix(
                renderOnTopTextures = jsSetOf(0, 1, 2, 18, 21, 24),
                hiddenObjects = jsSetOf(
                    "a_m_0_1i_1_isf1hw",
                    "a_m_0_1i_1_isfvf0",
                    "a_m_0_1i_1_ise7ew",
                    "a_m_0_1i_1_ishhj6",
                    "a_m_0_1i_1_isiw4p",
                    "a_m_0_1i_1_ishyp4",
                    "a_m_0_1i_1_isewhg",
                    "a_m_0_1i_1_isemhl",
                    "a_m_0_1i_1_isiuce",
                    "a_m_0_1i_1_isfvey",
                    "a_m_0_1i_1_isgolp",
                    "a_m_0_1i_1_iseg19",
                    "a_m_0_1i_1_isdzut",
                    "a_m_0_1i_1_isf0vs",
                    "a_m_0_1i_1_ishrwm",
                    "a_m_0_1i_1_isivaf",
                    "a_m_0_1i_1_isf0vs",
                    "a_m_0_1i_1_isfqe9",
                ),
            ),
            // Subterranean Desert 1
            Pair(Episode.IV, 6) to Fix(
                renderOnTopTextures = jsSetOf(48, 50, 58, 66, 80, 81, 92, 93, 94, 99, 100, 103),
                hiddenObjects = jsSetOf(
                    "s_v_f_16u_b_j2s5tx",
                    "s_v_d_84_f_j046sf",
                    "s_v_1v_205_2n_jb17vl",
                    "s_n_0_1s_1_iwnfqt",
                    "s_n_0_g1_6_iovjxw",
                    "s_v_d_z6_k_j1viu6",
                    "s_n_0_do_4_ipdh8p",
                    "s_v_c_7y_c_iu7yzc",
                    "s_v_8_4a_8_ixe9km",
                    "s_v_4_15_4_in60hf",
                    "s_n_0_6_1_ihtf3l",
                    "s_n_0_6_1_ikxbmr",
                    "s_v_9_3e_9_itbo7o",
                    "s_v_t_19k_r_iv3zbt",
                    "s_v_a_2s_a_ix4iob",
                    "s_v_b_37_b_iu5dp9",
                    "s_v_6_5t_7_iqx2nn",
                    "s_v_8_145_l_j0crhw",
                    "s_n_0_6_1_ikk5cn",
                    "s_v_5_15r_d_j2n06s",
                    "s_v_p_8n_p_j1enrp",
                    "s_v_b_p3_d_iu4vwf",
                    "s_v_c_3z_c_ithfqt",
                    "s_v_2_3g_2_itis48",
                    "s_v_17_h3_13_j7o59x",
                    "s_n_0_2t_1_iw2868",
                    "s_v_5_k1_8_ir35lp",
                    "s_v_h_7k_y_j5h3h2",
                    "s_v_8_4d_8_irrw8y",
                    "s_v_o_1qg_h_iyilpg", // Removes roof and walls but also some rocks in the middle.
                    "s_v_10_14y_11_j0vhyd",
                ),
            ),
        ).also {
            // VR Spaceship Beta = VR Spaceship Alpha
            it[Pair(Episode.II, 4)] = it[Pair(Episode.II, 3)]!!
            // Ep. IV Pioneer II = Ep. I Pioneer II
            it[Pair(Episode.IV, 0)] = it[Pair(Episode.I, 0)]!!
        }

        private val raycaster = Raycaster()
        private val tmpVec = Vector3()
        private val tmpIntersections = arrayOf<Intersection>()
    }
}
