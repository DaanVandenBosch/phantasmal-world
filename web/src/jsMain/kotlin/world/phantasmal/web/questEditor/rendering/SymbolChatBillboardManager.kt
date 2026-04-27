package world.phantasmal.web.questEditor.rendering

import kotlinx.browser.document
import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.cell.observeNow
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.psolib.fileFormats.quest.ObjectType
import world.phantasmal.web.core.rendering.disposeObject3DResources
import world.phantasmal.web.externals.three.*
import world.phantasmal.web.questEditor.loading.SymbolChatColliRepository
import world.phantasmal.web.questEditor.models.QuestObjectModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.web.questEditor.widgets.SymbolChatRenderer
import world.phantasmal.web.questEditor.widgets.SymbolChatStageRenderer
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.obj

/**
 * Renders a billboard above a selected SymbolChatObject showing every
 * in-range stage preset side-by-side, each badged S1/S2/S3 so the user
 * can tell which spec slot each thumbnail came from. The editor can't
 * know which stage the player's runtime switch state actually resolves
 * to, so showing all valid stages at once is more honest than picking
 * one.
 *
 * If all three slots are out-of-range sentinels (e.g. vanilla 1c2_e.qst
 * obj#40 with spec1=sc30), a gray "nothing at any stage" placeholder
 * takes the place of a single stage strip.
 *
 * The billboard faces the camera and keeps a constant screen size. Its
 * plane width scales with the number of visible stages, so a 3-stage
 * object visibly reads as "wider" in the viewport.
 *
 * Strip painting is delegated to [SymbolChatStageRenderer] so the ASM
 * editor's inline `set_symbol_chat_collision` preview shares the same
 * visual language.
 */
class SymbolChatBillboardManager(
    private val questEditorStore: QuestEditorStore,
    private val symbolChatColliRepository: SymbolChatColliRepository,
    private val renderContext: QuestRenderContext,
) : DisposableContainer() {
    private val offscreenCanvas = document.createElement("canvas") as HTMLCanvasElement
    private var billboardMesh: Mesh? = null
    private var billboardTexture: Texture? = null
    private val positionObserverDisposer = addDisposable(Disposer())

    // Scratch vector reused each frame by beforeRender() to avoid GC churn.
    private val tmpWorldPos = Vector3()

    init {
        observeNow(questEditorStore.selectedEntity) { entity ->
            clearBillboard()
            if (entity is QuestObjectModel && entity.type == ObjectType.SymbolChatObject) {
                createBillboard(entity)
            }
        }
    }

    /**
     * Called each frame to keep the billboard facing the camera and at
     * constant screen size. Uses quaternion copy instead of lookAt so the
     * billboard stays parallel to the view plane (no perspective tilt).
     */
    fun beforeRender() {
        billboardMesh?.let { mesh ->
            mesh.quaternion.copy(renderContext.camera.quaternion)
            mesh.asDynamic().getWorldPosition(tmpWorldPos)
            val distance = renderContext.camera.position.distanceTo(tmpWorldPos)
            val scaleFactor = (distance / BASE_DISTANCE).coerceIn(0.1, 10.0)
            mesh.scale.asDynamic().setScalar(scaleFactor)
        }
    }

    private fun createBillboard(entity: QuestObjectModel) {
        val stages = collectValidStages(entity)
        // paintStages resizes offscreenCanvas to fit `max(stages.size, 1)`
        // strips; we build the plane geometry right after so its aspect
        // matches the final canvas dimensions.
        SymbolChatStageRenderer.paintStages(offscreenCanvas, stages) {
            billboardTexture?.needsUpdate = true
        }

        val texture = Texture()
        texture.asDynamic().image = offscreenCanvas
        texture.needsUpdate = true
        billboardTexture = texture

        val geometry = planeGeometryFor(stages.size)
        val material = MeshBasicMaterial(obj {
            map = texture
            transparent = true
            alphaTest = 0.05
            side = DoubleSide
        })
        material.asDynamic().depthTest = false
        material.asDynamic().depthWrite = false

        val mesh = Mesh(geometry, material).apply {
            name = "SymbolChatBillboard"
            renderOrder = 9000
            frustumCulled = false
        }
        billboardMesh = mesh

        updateMeshPosition(entity)
        renderContext.helpers.add(mesh)

        // Track entity position changes.
        positionObserverDisposer.add(entity.worldPosition.observeNow { _ ->
            if (questEditorStore.selectedEntity.value == entity) {
                updateMeshPosition(entity)
            }
        })

        // Observe all three SC ID properties. Any id flipping between
        // sentinel and in-range can add/remove a stage strip — which
        // changes both the canvas size and the plane geometry, so we
        // refresh both if the stage count moves.
        for (name in SC_ID_PROP_NAMES) {
            val prop = entity.properties.value.find { it.name == name } ?: continue
            positionObserverDisposer.add(prop.value.observeNow { _ ->
                if (questEditorStore.selectedEntity.value == entity) {
                    refreshStages(entity)
                }
            })
        }
    }

    /**
     * Walks spec1 → spec2 → spec3 and returns a Stage for each whose SC ID
     * is in [0, ENTRY_COUNT). Out-of-range sentinels are skipped.
     */
    private fun collectValidStages(entity: QuestObjectModel): List<SymbolChatStageRenderer.Stage> {
        val props = entity.properties.value
        val out = mutableListOf<SymbolChatStageRenderer.Stage>()
        for ((index, name) in SC_ID_PROP_NAMES.withIndex()) {
            val prop = props.find { it.name == name } ?: continue
            val id = prop.value.value as? Int ?: continue
            val buf = symbolChatColliRepository.entry(id) ?: continue
            out.add(SymbolChatStageRenderer.Stage(slot = index + 1, buf = buf))
        }
        return out
    }

    private fun planeGeometryFor(stageCount: Int): PlaneGeometry {
        val columns = stageCount.coerceAtLeast(1)
        val stripHeight = BILLBOARD_WIDTH *
            SymbolChatRenderer.CANVAS_HEIGHT.toDouble() /
            SymbolChatRenderer.CANVAS_WIDTH.toDouble()
        return PlaneGeometry(BILLBOARD_WIDTH * columns, stripHeight)
    }

    /**
     * Recomputes stages and updates the billboard in place. If the stage
     * count changed, the plane geometry is swapped so the billboard's
     * world-space size matches its content. The canvas is always repainted.
     */
    private fun refreshStages(entity: QuestObjectModel) {
        val stages = collectValidStages(entity)
        val oldColumns = offscreenCanvas.width / SymbolChatRenderer.CANVAS_WIDTH
        val newColumns = stages.size.coerceAtLeast(1)
        if (newColumns != oldColumns) {
            billboardMesh?.let { mesh ->
                val oldGeom = mesh.geometry
                mesh.asDynamic().geometry = planeGeometryFor(stages.size)
                oldGeom.asDynamic().dispose()
            }
        }
        SymbolChatStageRenderer.paintStages(offscreenCanvas, stages) {
            billboardTexture?.needsUpdate = true
        }
    }

    private fun updateMeshPosition(entity: QuestObjectModel) {
        val pos = entity.worldPosition.value
        billboardMesh?.position?.set(pos.x, pos.y + BILLBOARD_Y_OFFSET, pos.z)
    }

    private fun clearBillboard() {
        positionObserverDisposer.disposeAll()
        billboardMesh?.let { mesh ->
            renderContext.helpers.remove(mesh)
            disposeObject3DResources(mesh)
        }
        billboardMesh = null
        billboardTexture?.dispose()
        billboardTexture = null
    }

    override fun dispose() {
        clearBillboard()
        super.dispose()
    }

    companion object {
        private const val BILLBOARD_WIDTH = 120.0
        private const val BILLBOARD_Y_OFFSET = 60.0
        private const val BASE_DISTANCE = 800.0
        private val SC_ID_PROP_NAMES = listOf("SC ID 1", "SC ID 2", "SC ID 3")
    }
}
