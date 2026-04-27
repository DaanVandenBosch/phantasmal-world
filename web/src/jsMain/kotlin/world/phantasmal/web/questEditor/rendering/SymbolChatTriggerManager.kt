package world.phantasmal.web.questEditor.rendering

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import world.phantasmal.cell.Cell
import world.phantasmal.core.disposable.disposable
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.web.core.rendering.disposeObject3DResources
import world.phantasmal.web.externals.three.Intersection
import world.phantasmal.web.externals.three.Object3D
import world.phantasmal.web.externals.three.Raycaster
import world.phantasmal.web.externals.three.Vector2
import world.phantasmal.web.questEditor.asm.SymbolChatTriggerInfo
import world.phantasmal.web.questEditor.loading.SymbolChatColliRepository
import world.phantasmal.web.questEditor.widgets.SymbolChatStageRenderer
import world.phantasmal.web.questEditor.widgets.resolveTriggerStages
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.dom.disposableListener

/**
 * Visualizes `set_symbol_chat_collision` triggers as semi-transparent orange
 * rings in the 3D viewport, plus a hover tooltip that paints the S1/S2/S3
 * stage strips so the user can see WHAT each trigger shows without
 * switching to the Script tab.
 *
 * Rings are rebuilt whenever [triggers] emits — which reacts to both
 * [QuestEditorStore.currentQuest] changes AND the current quest's
 * `bytecodeRevision` tick, so ASM-editor reassembles refresh the rings
 * without requiring a quest reload.
 */
class SymbolChatTriggerManager(
    triggers: Cell<List<SymbolChatTriggerInfo>>,
    private val readSegmentData: (Int) -> Buffer?,
    private val symbolChatColliRepository: SymbolChatColliRepository,
    private val renderContext: QuestRenderContext,
) : DisposableContainer() {
    private val rangeCircleRenderer = RangeCircleRenderer()

    // Rings added to renderContext.helpers, paired with the trigger they
    // came from so pointer hits can map back to trigger data.
    private data class RingEntry(val ring: Object3D, val trigger: SymbolChatTriggerInfo)
    private val ringEntries = mutableListOf<RingEntry>()

    private val raycaster = Raycaster()
    private val pointer = Vector2()
    // Reusable intersection buffer — the three.js API wants an optional
    // target array and populates it in place. Avoid allocating per move.
    private val raycastHits = arrayOf<Intersection>()
    private var hoveredTrigger: SymbolChatTriggerInfo? = null

    private val tooltipDom: HTMLDivElement = document.createElement("div") as HTMLDivElement
    private val tooltipCanvas: HTMLCanvasElement = document.createElement("canvas") as HTMLCanvasElement

    init {
        // Inline styles instead of a CSS class so this DisposableContainer
        // doesn't need to touch Widget.Companion.style (which is protected
        // and not reachable from here).
        tooltipDom.style.apply {
            position = "fixed"
            zIndex = "2000"
            padding = "3px"
            background = "rgba(30, 30, 30, 0.92)"
            border = "1px solid hsl(35, 95%, 50%)"
            setProperty("box-shadow", "0 2px 8px rgba(0, 0, 0, 0.6)")
            setProperty("pointer-events", "none")
            display = "none"
        }
        tooltipCanvas.style.apply {
            setProperty("image-rendering", "pixelated")
            height = "60px"
            background = "#181818"
            display = "block"
        }
        tooltipDom.appendChild(tooltipCanvas)
        // Body-level append so the tooltip escapes the canvas container's
        // overflow and stays on top of other editor chrome.
        document.body?.appendChild(tooltipDom)
        addDisposable(disposable { tooltipDom.remove() })

        observeNow(triggers) { newTriggers ->
            clearRings()
            createRings(newTriggers)
        }

        // Hover tracking. pointermove runs often so keep the raycast scoped
        // to our small ring list — checking against `renderContext.helpers`
        // as a whole would include every label / helper / billboard.
        addDisposable(renderContext.canvas.disposableListener("pointermove", { ev: Event ->
            handlePointerMove(ev as MouseEvent)
        }))
        addDisposable(renderContext.canvas.disposableListener("pointerleave", { _: Event ->
            hideTooltip()
        }))
    }

    private fun createRings(triggers: List<SymbolChatTriggerInfo>) {
        for (trigger in triggers) {
            val ring = rangeCircleRenderer.createRangeCircle(
                centerX = trigger.x,
                centerY = trigger.y,
                centerZ = trigger.z,
                radius = trigger.radius,
                color = TRIGGER_COLOR,
            )
            ring.name = "SymbolChatTrigger"
            renderContext.helpers.add(ring)
            ringEntries.add(RingEntry(ring = ring, trigger = trigger))
        }
    }

    private fun clearRings() {
        for (entry in ringEntries) {
            renderContext.helpers.remove(entry.ring)
            disposeObject3DResources(entry.ring)
        }
        ringEntries.clear()
        hoveredTrigger = null
        hideTooltip()
    }

    private fun handlePointerMove(ev: MouseEvent) {
        if (ringEntries.isEmpty()) {
            if (hoveredTrigger != null) hideTooltip()
            return
        }

        val canvas = renderContext.canvas
        val rect = canvas.getBoundingClientRect()
        if (rect.width <= 0 || rect.height <= 0) return
        pointer.x = ((ev.clientX - rect.left) / rect.width) * 2 - 1
        pointer.y = -((ev.clientY - rect.top) / rect.height) * 2 + 1
        raycaster.setFromCamera(pointer, renderContext.camera)

        var hitTrigger: SymbolChatTriggerInfo? = null
        var nearestDistance = Double.POSITIVE_INFINITY
        for (entry in ringEntries) {
            raycastHits.asDynamic().length = 0
            raycaster.intersectObject(entry.ring, recursive = true, raycastHits)
            val first = raycastHits.firstOrNull() ?: continue
            if (first.distance < nearestDistance) {
                nearestDistance = first.distance
                hitTrigger = entry.trigger
            }
        }

        if (hitTrigger != null) {
            if (hitTrigger !== hoveredTrigger) {
                hoveredTrigger = hitTrigger
                repaintTooltip(hitTrigger)
            }
            // Show before measuring so getBoundingClientRect sees real
            // dimensions. Repositioning while display:none returns zeros.
            tooltipDom.style.display = "block"
            positionTooltip(ev.clientX, ev.clientY)
        } else if (hoveredTrigger != null) {
            hideTooltip()
        }
    }

    private fun repaintTooltip(trigger: SymbolChatTriggerInfo) {
        val stages = resolveTriggerStages(trigger, readSegmentData, symbolChatColliRepository)
        SymbolChatStageRenderer.paintStages(tooltipCanvas, stages)
    }

    private fun positionTooltip(clientX: Int, clientY: Int) {
        // Default position: below-right of the cursor, offset so the tooltip
        // isn't under the pointer (which would intercept the same moves and
        // cause flicker — pointer-events:none on the tooltip also protects,
        // but staying out of the path is cleaner).
        val offset = 14
        var left = clientX + offset
        var top = clientY + offset
        tooltipDom.style.left = "${left}px"
        tooltipDom.style.top = "${top}px"

        // Measure with the style applied and flip sides when the tooltip
        // would overflow the viewport. Fall back to clamping to the viewport
        // edge when even the flipped position would overflow (tooltip wider
        // or taller than the viewport — unlikely but possible on tiny screens
        // with a 3-stage tooltip).
        val rect = tooltipDom.getBoundingClientRect()
        val vw = window.innerWidth.toDouble()
        val vh = window.innerHeight.toDouble()

        if (rect.right > vw) {
            left = (clientX - rect.width.toInt() - offset).coerceAtLeast(0)
            tooltipDom.style.left = "${left}px"
        }
        if (rect.bottom > vh) {
            top = (clientY - rect.height.toInt() - offset).coerceAtLeast(0)
            tooltipDom.style.top = "${top}px"
        }
    }

    private fun hideTooltip() {
        hoveredTrigger = null
        tooltipDom.style.display = "none"
    }

    override fun dispose() {
        clearRings()
        super.dispose()
    }

    companion object {
        private const val TRIGGER_COLOR = 0xFFAA00 // Orange
    }
}
