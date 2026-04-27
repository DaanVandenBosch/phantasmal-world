package world.phantasmal.web.questEditor.rendering

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.cell.Cell
import world.phantasmal.psolib.fileFormats.ninja.NjObject
import world.phantasmal.psolib.fileFormats.ninja.XvrTexture
import world.phantasmal.web.core.boundingSphere
import world.phantasmal.web.core.rendering.DisposableThreeRenderer
import world.phantasmal.web.core.rendering.OrbitalCameraInputManager
import world.phantasmal.web.core.rendering.RenderContext
import world.phantasmal.web.core.rendering.Renderer
import world.phantasmal.web.core.rendering.conversion.ninjaObjectToSkinnedMesh
import world.phantasmal.web.core.rendering.disposeObject3DResources
import world.phantasmal.web.externals.three.Group
import world.phantasmal.web.externals.three.Object3D
import world.phantasmal.web.externals.three.PerspectiveCamera
import world.phantasmal.web.externals.three.Vector3
import world.phantasmal.web.shared.dto.SectionId
import world.phantasmal.web.viewer.loading.CharacterClassAssetLoader
import world.phantasmal.web.viewer.models.CharacterClass
import world.phantasmal.web.viewer.models.CharacterClass.*

/** Map PSO char_class byte (0-11) to CharacterClass enum. */
private val CHAR_CLASS_BY_INDEX = arrayOf(
    HUmar, HUnewearl, HUcast, RAmar, RAcast, RAcaseal,
    FOmarl, FOnewm, FOnewearl, HUcaseal, FOmar, RAmarl,
)

private val logger = KotlinLogging.logger {}

class NpcPreviewRenderer(
    private val charClassAssetLoader: CharacterClassAssetLoader,
    createThreeRenderer: (HTMLCanvasElement) -> DisposableThreeRenderer,
    private val charClassCell: Cell<Int>,
    private val sectionIdCell: Cell<Int>,   // 1-indexed
    private val costumeCell: Cell<Int>,     // 1-indexed
    private val skinCell: Cell<Int>,        // 1-indexed
    private val faceCell: Cell<Int>,        // 1-indexed
    private val headCell: Cell<Int>,        // 1-indexed
    private val hairCell: Cell<Int>,        // 1-indexed
    private val v2FlagsCell: Cell<Int>,     // 0 = player model, != 0 = NPC model
    private val extraModelCell: Cell<Int>,  // 0-6: GM, Rico, Sonic, Knux, Tails, Flowen, Elly
) : Renderer() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loadJob: Job? = null
    private var currentMesh: Object3D? = null

    override val context = addDisposable(
        RenderContext(
            createCanvas(),
            PerspectiveCamera(
                fov = 45.0,
                aspect = 1.0,
                near = 1.0,
                far = 5_000.0,
            ),
        )
    )

    override val threeRenderer = addDisposable(createThreeRenderer(context.canvas)).renderer

    override val inputManager = addDisposable(
        OrbitalCameraInputManager(
            context.canvas,
            context.camera,
            position = Vector3(.0, .0, .0),
            screenSpacePanning = true,
        )
    )

    /**
     * Call this whenever the NPC appearance fields change to reload the model.
     */
    fun refresh() {
        rebuildModel()
    }

    override fun render() {
        context.lightHolder.quaternion.copy(context.camera.quaternion)
        super.render()
    }

    override fun dispose() {
        loadJob?.cancel()
        removeMesh()
        super.dispose()
    }

    private fun removeMesh() {
        currentMesh?.let {
            disposeObject3DResources(it)
            context.scene.remove(it)
            currentMesh = null
        }
    }

    private fun rebuildModel() {
        loadJob?.cancel()

        if (v2FlagsCell.value != 0) {
            // NPC model (GM, Rico, Sonic, etc.)
            val extraModel = extraModelCell.value
            val sectionId = (sectionIdCell.value - 1).coerceAtLeast(0)
            loadJob = scope.launch {
                try {
                    val parts = charClassAssetLoader.loadNpcParts(extraModel)
                    val allTextures = charClassAssetLoader.loadNpcXvrTextures(extraModel)

                    removeMesh()

                    val group = Group()
                    val anisotropy = threeRenderer.capabilities.getMaxAnisotropy() / 2

                    // Body
                    if (parts.isNotEmpty()) {
                        val bodyTexList = buildNpcBodyTextures(extraModel, sectionId, allTextures)
                        val bodyMesh = ninjaObjectToSkinnedMesh(
                            parts[0], bodyTexList, boundingVolumes = true, anisotropy = anisotropy,
                        )
                        group.add(bodyMesh)
                    }

                    // Head
                    if (parts.size > 1) {
                        val headTexList = buildNpcHeadTextures(extraModel, allTextures)
                        val headMesh = ninjaObjectToSkinnedMesh(
                            parts[1], headTexList, boundingVolumes = true, anisotropy = anisotropy,
                        )
                        headMesh.position.y = NPC_HEAD_Y.getOrElse(extraModel) { 15.7 }
                        group.add(headMesh)
                    }

                    // Hair (Flowen/Elly only)
                    if (parts.size > 2) {
                        val hairTexList = buildNpcHairTextures(extraModel, allTextures)
                        val hairMesh = ninjaObjectToSkinnedMesh(
                            parts[2], hairTexList, boundingVolumes = true, anisotropy = anisotropy,
                        )
                        hairMesh.position.y = NPC_HEAD_Y.getOrElse(extraModel) { 15.7 }
                        group.add(hairMesh)
                    }

                    positionCameraAndShow(group)
                } catch (e: Exception) {
                    logger.warn(e) { "Failed to load NPC model for extra_model=$extraModel." }
                    removeMesh()
                }
            }
            return
        }

        val classIdx = charClassCell.value
        val charClass = CHAR_CLASS_BY_INDEX.getOrNull(classIdx)
        if (charClass == null) {
            logger.warn { "Invalid char_class index for NPC preview: $classIdx" }
            removeMesh()
            return
        }
        val isCast = charClass.hairStyleCount == 0
        val headStyle = (headCell.value - 1).coerceIn(0, charClass.headStyleCount - 1)
        val hairStyle = (hairCell.value - 1).coerceIn(0, (charClass.hairStyleCount - 1).coerceAtLeast(0))
        val sectionId = SectionId.VALUES.getOrNull((sectionIdCell.value - 1).coerceAtLeast(0))
            ?: SectionId.Viridia
        // Cast: body textures use skin; Non-cast: body textures use costume
        val body = if (isCast) (skinCell.value - 1).coerceAtLeast(0)
                   else (costumeCell.value - 1).coerceAtLeast(0)
        val skin = (skinCell.value - 1).coerceAtLeast(0)
        val face = (faceCell.value - 1).coerceAtLeast(0)

        loadJob = scope.launch {
            try {
                val njObject = charClassAssetLoader.loadNinjaObject(charClass, headStyle, hairStyle)
                val textures = charClassAssetLoader.loadXvrTextures(
                    charClass, sectionId, body, skin, face, headStyle,
                )

                removeMesh()

                val mesh = ninjaObjectToSkinnedMesh(
                    njObject,
                    textures,
                    boundingVolumes = true,
                    anisotropy = threeRenderer.capabilities.getMaxAnisotropy() / 2,
                )

                positionCameraAndShow(mesh)
            } catch (e: Exception) {
                logger.warn(e) { "Failed to load NPC preview model for $charClass." }
            }
        }
    }

    private fun positionCameraAndShow(mesh: Object3D) {
        // Ensure world matrices are up-to-date for bounding sphere calculation.
        mesh.updateMatrixWorld(true)
        val bSphere = boundingSphere(mesh)
        val center = bSphere.center
        val dist = bSphere.radius * 3.0
        val cameraPos = Vector3(center.x, center.y, center.z + dist)
        inputManager.lookAt(cameraPos, center)

        context.scene.add(mesh)
        currentMesh = mesh
    }

    /**
     * Build texture list for NPC body, mapping texture slots to AFS entries.
     * Based on qedit NPCBuild.pas BuildNPC procedure.
     */
    private fun buildNpcBodyTextures(
        extraModel: Int,
        sectionId: Int,
        afs: List<XvrTexture?>,
    ): List<XvrTexture?> {
        val size = afs.size
        fun get(i: Int): XvrTexture? = if (i in 0 until size) afs[i] else null

        // Section ID texture offset differs per NPC.
        val sectionIdIdx = when {
            extraModel < 5 -> 3 + sectionId   // GM, Rico, Sonic, Knux, Tails
            extraModel == 5 -> 6 + sectionId   // Flowen
            else -> 5 + sectionId              // Elly
        }

        return when (extraModel) {
            5 -> {
                // Flowen: slots 0=section, 1=afs1, 2=afs2, 3=afs0, 4=afs3
                val list = MutableList<XvrTexture?>(5) { null }
                list[0] = get(sectionIdIdx)
                list[1] = get(1)
                list[2] = get(2)
                list[3] = get(0)
                list[4] = get(3)
                list
            }
            6 -> {
                // Elly: slots 0=section, 1=afs1, 2=afs2, 3=afs0
                val list = MutableList<XvrTexture?>(4) { null }
                list[0] = get(sectionIdIdx)
                list[1] = get(1)
                list[2] = get(2)
                list[3] = get(0)
                list
            }
            else -> {
                // GM, Rico, Sonic, Knux, Tails: slots 0=section, 1=afs0, 2=afs1
                val list = MutableList<XvrTexture?>(3) { null }
                list[0] = get(sectionIdIdx)
                list[1] = get(0)
                list[2] = get(1)
                list
            }
        }
    }

    private fun buildNpcHeadTextures(
        extraModel: Int,
        afs: List<XvrTexture?>,
    ): List<XvrTexture?> {
        val size = afs.size
        fun get(i: Int): XvrTexture? = if (i in 0 until size) afs[i] else null

        return when (extraModel) {
            0 -> {
                // GM: slot 0=afs2, slot 1=afs1
                val list = MutableList<XvrTexture?>(2) { null }
                list[0] = get(2)
                list[1] = get(1)
                list
            }
            5 -> {
                // Flowen: slot 0=afs5, slot 1=afs4
                val list = MutableList<XvrTexture?>(2) { null }
                list[0] = get(5)
                list[1] = get(4)
                list
            }
            6 -> {
                // Elly: slot 0=afs3
                val list = MutableList<XvrTexture?>(1) { null }
                list[0] = get(3)
                list
            }
            else -> {
                // Rico, Sonic, Knux, Tails: slot 0=afs1, slot 1=afs2
                val list = MutableList<XvrTexture?>(2) { null }
                list[0] = get(1)
                list[1] = get(2)
                list
            }
        }
    }

    private fun buildNpcHairTextures(
        extraModel: Int,
        afs: List<XvrTexture?>,
    ): List<XvrTexture?> {
        val size = afs.size
        fun get(i: Int): XvrTexture? = if (i in 0 until size) afs[i] else null

        // Hair only for Flowen (5) and Elly (6). qedit sets slot 3.
        val list = MutableList<XvrTexture?>(4) { null }
        list[3] = when (extraModel) {
            5 -> get(5)  // Flowen
            6 -> get(4)  // Elly
            else -> null
        }
        return list
    }

    companion object {
        /** Head Y positions per NPC, from qedit. */
        private val NPC_HEAD_Y = doubleArrayOf(
            15.7,  // 0 = GM
            14.5,  // 1 = Rico
            7.3,   // 2 = Sonic
            7.9,   // 3 = Knux
            5.0,   // 4 = Tails
            17.0,  // 5 = Flowen
            14.5,  // 6 = Elly
        )
    }
}
