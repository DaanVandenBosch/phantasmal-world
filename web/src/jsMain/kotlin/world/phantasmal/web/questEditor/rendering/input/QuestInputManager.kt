package world.phantasmal.web.questEditor.rendering.input

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.FocusEvent
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.pointerevents.PointerEvent
import world.phantasmal.cell.mutateDeferred
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.web.core.rendering.InputManager
import world.phantasmal.web.core.rendering.OrbitalCameraInputManager
import world.phantasmal.web.externals.three.Plane
import world.phantasmal.web.externals.three.Raycaster
import world.phantasmal.web.externals.three.Vector2
import world.phantasmal.web.externals.three.Vector3
import world.phantasmal.web.questEditor.rendering.QuestRenderContext
import world.phantasmal.web.questEditor.rendering.input.state.IdleState
import world.phantasmal.web.questEditor.rendering.input.state.State
import world.phantasmal.web.questEditor.rendering.input.state.StateContext
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.web.questEditor.stores.QuestEditorUiStore
import world.phantasmal.web.questEditor.stores.ViewportStore
import world.phantasmal.web.questEditor.widgets.*
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.dom.disposableListener

class QuestInputManager(
    private val questEditorStore: QuestEditorStore,
    private val questEditorUiStore: QuestEditorUiStore,
    private val viewportStore: ViewportStore,
    private val renderContext: QuestRenderContext,
) : DisposableContainer(), InputManager {
    private val stateContext: StateContext
    private val pointerPosition = Vector2()
    private val pointerDevicePosition = Vector2()
    private val lastPointerPosition = Vector2()
    private var movedSinceLastPointerDown = false
    private var state: State
    private var onPointerUpListener: Disposable? = null
    private var onPointerMoveListener: Disposable? = null
    private var contextMenuListener: Disposable? = null
    private val pointerDragging: Boolean get() = onPointerUpListener != null

    /**
     * Prevents events from triggering while dragging the pointer.
     */
    private val pointerTrap = document.createElement("div") as HTMLElement

    private val raycaster = Raycaster()
    private val groundPlane = Plane(Vector3(0.0, 1.0, 0.0), 0.0) // Y = 0 plane

    private val cameraInputManager: OrbitalCameraInputManager

    /**
     * Whether entity transformations, deletions, etc. are enabled or not.
     * Hover over and selection still work when this is set to false.
     */
    private var entityManipulationEnabled: Boolean = true
        set(enabled) {
            field = enabled
            returnToIdleState()
        }

    /** True until the first camera navigation, used to decide whether to apply a fixed offset. */
    private var cameraIsAtInitialPosition: Boolean = true

    init {
        onPointerMoveListener =
            renderContext.canvas.disposableListener("pointermove", ::onPointerMove)

        addDisposables(
            renderContext.canvas.disposableListener<FocusEvent>(
                "focus",
                { onFocus() },
                useCapture = true,
            ),
            renderContext.canvas.disposableListener("pointerdown", ::onPointerDown),
            renderContext.canvas.disposableListener("pointerout", ::onPointerOut),
            renderContext.canvas.disposableListener("pointercancel", ::onPointerCancel),
            renderContext.canvas.disposableListener("keydown", ::onKeyDown),
            renderContext.canvas.observeEntityDragEnter(::onEntityDragEnter),
            renderContext.canvas.observeEntityDragOver(::onEntityDragOver),
            renderContext.canvas.observeEntityDragLeave(::onEntityDragLeave),
            renderContext.canvas.observeEntityDrop(::onEntityDrop),
        )

        // Ensure OrbitalCameraControls attaches its listeners after we've attached ours.
        cameraInputManager = OrbitalCameraInputManager(
            renderContext.canvas,
            renderContext.camera,
            position = Vector3(0.0, 800.0, 700.0),
            screenSpacePanning = false,
        )

        stateContext = StateContext(questEditorStore, questEditorUiStore, renderContext, cameraInputManager)
        state = IdleState(stateContext, entityManipulationEnabled)

        // Observe quest editing enabled state
        observeNow(questEditorStore.questEditingEnabled) { entityManipulationEnabled = it }

        // Reset initial camera flag when a new quest is loaded.
        observe(questEditorStore.currentQuest) { cameraIsAtInitialPosition = true }
        
        // Observe target camera position for navigation — preserves current zoom level.
        // After handling, immediately clear the cell so subsequent navigations to the same
        // position still trigger the observer (SimpleCell deduplicates equal values).
        observe(viewportStore.targetCameraPosition) { targetPosition ->
            targetPosition?.let { position ->
                if (cameraIsAtInitialPosition) {
                    cameraIsAtInitialPosition = false
                    val cameraOffset = Vector3(0.0, 600.0, 900.0)
                    val cameraPosition = position.clone().add(cameraOffset)
                    cameraInputManager.lookAt(cameraPosition, position)
                } else {
                    val currentFloorId = getCurrentFloorId()
                    cameraInputManager.lookAtPreservingViewpoint(position, currentFloorId)
                }

                mutateDeferred { viewportStore.setTargetCameraPosition(null) }
            }
        }

        pointerTrap.className = "pw-quest-editor-input-manager-pointer-trap"
        pointerTrap.hidden = true
        pointerTrap.style.zIndex = "1000"
        pointerTrap.style.position = "fixed"
        pointerTrap.style.left = "0"
        pointerTrap.style.top = "0"
        pointerTrap.style.width = "100%"
        pointerTrap.style.height = "100%"
        pointerTrap.addEventListener("contextmenu", ::onContextMenu)

        window.document.body?.appendChild(pointerTrap)
    }

    override fun dispose() {
        cameraInputManager.dispose()
        onPointerUpListener?.dispose()
        onPointerMoveListener?.dispose()
        contextMenuListener?.dispose()
        window.document.body?.removeChild(pointerTrap)
        super.dispose()
    }

    override fun setSize(width: Int, height: Int) {
        cameraInputManager.setSize(width, height)
    }

    override fun resetCamera() {
        cameraInputManager.resetCamera()
    }

    override fun beforeRender() {
        state.beforeRender()
        cameraInputManager.beforeRender()

        // Update user offset when camera controls change the target (e.g., during pan operations)
        cameraInputManager.updateUserOffset()
    }

    private fun onFocus() {
        questEditorStore.makeMainUndoCurrent()
    }

    private fun onPointerDown(e: PointerEvent) {
        viewportStore.dismissContextMenu()

        processPointerEvent(e)

        state = state.processEvent(
            PointerDownEvt(
                e.buttons.toInt(),
                ctrlKey = e.ctrlKey,
                shiftKey = e.shiftKey,
                pointerDevicePosition,
                movedSinceLastPointerDown,
            )
        )

        onPointerUpListener = window.disposableListener("pointerup", ::onPointerUp)

        // Stop listening to canvas move events and start listening to window move events.
        onPointerMoveListener?.dispose()
        onPointerMoveListener = window.disposableListener("pointermove", ::onPointerMove)

        pointerTrap.hidden = false
        // Add this listener in addition to the pointer trap to avoid context menu from triggering
        // when dragging and releasing the pointer in a different window.
        if (contextMenuListener == null) {
            contextMenuListener = window.disposableListener("contextmenu", ::onContextMenu)
        }
    }

    private fun onPointerUp(e: PointerEvent) {
        try {
            processPointerEvent(e)

            state = state.processEvent(
                PointerUpEvt(
                    e.buttons.toInt(),
                    ctrlKey = e.ctrlKey,
                    shiftKey = e.shiftKey,
                    pointerDevicePosition,
                    movedSinceLastPointerDown,
                )
            )

            // Show context menu on single right-click (no drag).
            // Temporarily disable OrbitControls to prevent the click from applying a rotation.
            if (e.button.toInt() == 2 && !movedSinceLastPointerDown) {
                cameraInputManager.enabled = false
                cameraInputManager.enabled = true
                viewportStore.requestContextMenu(e.clientX, e.clientY)
            }
        } finally {
            onPointerUpListener?.dispose()
            onPointerUpListener = null

            // Stop listening to window move events and start listening to canvas move events again.
            onPointerMoveListener?.dispose()
            onPointerMoveListener =
                renderContext.canvas.disposableListener("pointermove", ::onPointerMove)

            window.setTimeout({
                if (disposed) return@setTimeout
                if (!pointerDragging) {
                    pointerTrap.hidden = true
                    contextMenuListener?.dispose()
                    contextMenuListener = null
                }
            }, 0)
        }
    }

    private fun onPointerMove(e: PointerEvent) {
        processPointerEvent(e)

        state = state.processEvent(
            PointerMoveEvt(
                e.buttons.toInt(),
                ctrlKey = e.ctrlKey,
                shiftKey = e.shiftKey,
                pointerDevicePosition,
                movedSinceLastPointerDown,
            )
        )
    }

    private fun onPointerOut(e: PointerEvent) {
        processPointerEvent(type = null, e.clientX, e.clientY)

        // Clear mouse world position when pointer leaves canvas
        viewportStore.setMouseWorldPosition(null)

        state = state.processEvent(
            PointerOutEvt(
                e.buttons.toInt(),
                ctrlKey = e.ctrlKey,
                shiftKey = e.shiftKey,
                pointerDevicePosition,
                movedSinceLastPointerDown,
            )
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onPointerCancel(e: PointerEvent) {
        returnToIdleState()
    }

    private fun onKeyDown(e: KeyboardEvent) {
        state = state.processEvent(KeyboardEvt(e.key))
    }

    private fun onEntityDragEnter(e: EntityDragEvent) {
        processPointerEvent(type = null, e.clientX, e.clientY)

        state = state.processEvent(EntityDragEnterEvt(e, pointerDevicePosition))
    }

    private fun onEntityDragOver(e: EntityDragEvent) {
        processPointerEvent(type = null, e.clientX, e.clientY)

        state = state.processEvent(EntityDragOverEvt(e, pointerDevicePosition))
    }

    private fun onEntityDragLeave(e: EntityDragEvent) {
        processPointerEvent(type = null, e.clientX, e.clientY)

        state = state.processEvent(EntityDragLeaveEvt(e, pointerDevicePosition))
    }

    private fun onEntityDrop(e: EntityDragEvent) {
        processPointerEvent(type = null, e.clientX, e.clientY)

        state = state.processEvent(EntityDropEvt(e, pointerDevicePosition))
    }

    // Avoid context menu from popping up when dragging and releasing mouse outside of 3D view.
    private fun onContextMenu(e: Event) {
        e.preventDefault()
        e.stopPropagation()
    }

    private fun processPointerEvent(e: PointerEvent) {
        e.stopPropagation()

        processPointerEvent(e.type, e.clientX, e.clientY)
    }

    private fun processPointerEvent(type: String?, clientX: Int, clientY: Int) {
        val rect = renderContext.canvas.getBoundingClientRect()
        pointerPosition.set(clientX - rect.left, clientY - rect.top)
        pointerDevicePosition.copy(pointerPosition)
        renderContext.pointerPosToDeviceCoords(pointerDevicePosition)

        // Calculate world position using raycaster
        updateMouseWorldPosition()

        when (type) {
            "pointerdown" -> {
                movedSinceLastPointerDown = false
            }
            "pointermove", "pointerup" -> {
                if (!pointerPosition.equals(lastPointerPosition)) {
                    movedSinceLastPointerDown = true
                }
            }
        }

        lastPointerPosition.copy(pointerPosition)
    }

    /** Reusable Vector3 for ground-plane intersection to avoid per-event allocation. */
    private val intersectionPoint = Vector3()

    private fun updateMouseWorldPosition() {
        try {
            // Pick the actual scene geometry so the readout reflects real terrain height
            // (PSO maps almost never sit at Y=0). Try collision geometry first because it's
            // the canonical "where the floor is" data, then render geometry, then fall back
            // to the Y=0 plane only when the ray misses geometry entirely (e.g. pointing
            // into the sky).
            val hit = stateContext.intersectObject(
                pointerDevicePosition,
                renderContext.collisionGeometry,
            ) ?: stateContext.intersectObject(
                pointerDevicePosition,
                renderContext.renderGeometry,
            )

            val worldPos = if (hit != null) {
                hit.point.clone()
            } else {
                raycaster.setFromCamera(pointerDevicePosition, renderContext.camera)
                raycaster.ray.intersectPlane(groundPlane, intersectionPoint)?.clone()
            }

            viewportStore.setMouseWorldPosition(worldPos)
        } catch (e: Exception) {
            // If there's any error, clear the position
            viewportStore.setMouseWorldPosition(null)
        }
    }

    private fun getCurrentFloorId(): Int? {
        val quest = questEditorStore.currentQuest.value ?: return null
        val currentVariant = questEditorStore.currentAreaVariant.value ?: return null

        // For quests with floor mappings, find the floor ID that matches current area and variant.
        // Fall back to area ID if no mapping matches, to avoid returning null which would cause
        // unnecessary camera offset resets.
        if (quest.floorMappings.isNotEmpty()) {
            val floorMapping = quest.floorMappings.find { mapping ->
                mapping.areaId == currentVariant.area.id && mapping.variantId == currentVariant.id
            }
            return floorMapping?.floorId ?: currentVariant.area.id
        }

        return currentVariant.area.id
    }

    private fun returnToIdleState() {
        if (state !is IdleState) {
            state.cancel()
            state = IdleState(stateContext, entityManipulationEnabled)
        }
    }
}
