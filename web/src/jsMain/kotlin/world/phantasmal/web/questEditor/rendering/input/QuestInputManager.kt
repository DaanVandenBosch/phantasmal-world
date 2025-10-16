package world.phantasmal.web.questEditor.rendering.input

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.FocusEvent
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.pointerevents.PointerEvent
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
import world.phantasmal.web.questEditor.widgets.*
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.dom.disposableListener

class QuestInputManager(
    private val questEditorStore: QuestEditorStore,
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

        stateContext = StateContext(questEditorStore, renderContext, cameraInputManager)
        state = IdleState(stateContext, entityManipulationEnabled)

        // Observe quest editing enabled state
        observeNow(questEditorStore.questEditingEnabled) { entityManipulationEnabled = it }
        
        // Observe target camera position for navigation - preserves current zoom level
        observeNow(questEditorStore.targetCameraPosition) { targetPosition ->
            targetPosition?.let { position ->
                // Check if camera has been initialized (not at default position)
                val currentDistance = renderContext.camera.position.distanceTo(cameraInputManager.controls.target)
                val isInitialState = currentDistance > 1400.0 // Initial distance is ~1200

                if (isInitialState) {
                    // For initial navigation, use the original approach with fixed offset
                    val cameraOffset = Vector3(0.0, 600.0, 900.0)
                    val cameraPosition = position.clone().add(cameraOffset)
                    console.log("Initial navigation - Moving camera to: ${cameraPosition.x}, ${cameraPosition.y}, ${cameraPosition.z}")
                    cameraInputManager.lookAt(cameraPosition, position)
                } else {
                    // For subsequent navigations, preserve the current viewpoint (camera position and orientation)
                    // Get current floor ID from quest and area variant
                    val currentFloorId = getCurrentFloorId()
                    cameraInputManager.lookAtPreservingViewpoint(position, currentFloorId)
                }

                // Clear the target position after navigation to allow future navigations
                window.setTimeout({
                    questEditorStore.setTargetCameraPosition(null)
                }, 200)
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
        questEditorStore.setMouseWorldPosition(null)

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

    private fun updateMouseWorldPosition() {
        try {
            // Set up raycaster from camera through mouse position
            raycaster.setFromCamera(pointerDevicePosition, renderContext.camera)

            // Intersect with ground plane (Y = 0)
            val intersectionPoint = Vector3()
            val intersected = raycaster.ray.intersectPlane(groundPlane, intersectionPoint)

            if (intersected != null) {
                questEditorStore.setMouseWorldPosition(intersected)
            } else {
                questEditorStore.setMouseWorldPosition(null)
            }
        } catch (e: Exception) {
            // If there's any error, clear the position
            questEditorStore.setMouseWorldPosition(null)
        }
    }

    private fun getCurrentFloorId(): Int? {
        val quest = questEditorStore.currentQuest.value ?: return null
        val currentVariant = questEditorStore.currentAreaVariant.value ?: return null

        // For quests with floor mappings, find the floor ID that matches current area and variant
        if (quest.floorMappings.isNotEmpty()) {
            val floorMapping = quest.floorMappings.find { mapping ->
                mapping.areaId == currentVariant.area.id && mapping.variantId == currentVariant.id
            }
            return floorMapping?.floorId
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
