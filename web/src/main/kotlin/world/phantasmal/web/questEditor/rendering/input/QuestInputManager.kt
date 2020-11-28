package world.phantasmal.web.questEditor.rendering.input

import kotlinx.browser.window
import org.w3c.dom.pointerevents.PointerEvent
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.web.core.rendering.InputManager
import world.phantasmal.web.core.rendering.OrbitalCameraInputManager
import world.phantasmal.web.externals.three.Vector2
import world.phantasmal.web.externals.three.Vector3
import world.phantasmal.web.questEditor.rendering.QuestRenderContext
import world.phantasmal.web.questEditor.rendering.input.state.IdleState
import world.phantasmal.web.questEditor.rendering.input.state.State
import world.phantasmal.web.questEditor.rendering.input.state.StateContext
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.dom.disposableListener

class QuestInputManager(
    questEditorStore: QuestEditorStore,
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
        addDisposables(
            disposableListener(renderContext.canvas, "pointerdown", ::onPointerDown)
        )

        onPointerMoveListener =
            disposableListener(renderContext.canvas, "pointermove", ::onPointerMove)

        // Ensure OrbitalCameraControls attaches its listeners after ours.
        cameraInputManager = OrbitalCameraInputManager(
            renderContext.canvas,
            renderContext.camera,
            position = Vector3(0.0, 800.0, 700.0),
            screenSpacePanning = false,
        )

        stateContext = StateContext(questEditorStore, renderContext, cameraInputManager)
        state = IdleState(stateContext, entityManipulationEnabled)

        observe(questEditorStore.selectedEntity) { returnToIdleState() }
        observe(questEditorStore.questEditingEnabled) { entityManipulationEnabled = it }
    }

    override fun internalDispose() {
        cameraInputManager.dispose()
        onPointerUpListener?.dispose()
        onPointerMoveListener?.dispose()
        super.internalDispose()
    }

    override fun setSize(width: Double, height: Double) {
        cameraInputManager.setSize(width, height)
    }

    override fun resetCamera() {
        cameraInputManager.resetCamera()
    }

    override fun beforeRender() {
        state.beforeRender()
        cameraInputManager.beforeRender()
    }

    private fun onPointerDown(e: PointerEvent) {
        processPointerEvent(e)

        state = state.processEvent(
            PointerDownEvt(
                e.buttons.toInt(),
                shiftKeyDown = e.shiftKey,
                movedSinceLastPointerDown,
                pointerDevicePosition,
            )
        )

        onPointerUpListener = disposableListener(window, "pointerup", ::onPointerUp)

        // Stop listening to canvas move events and start listening to window move events.
        onPointerMoveListener?.dispose()
        onPointerMoveListener = disposableListener(window, "pointermove", ::onPointerMove)
    }

    private fun onPointerUp(e: PointerEvent) {
        try {
            processPointerEvent(e)

            state = state.processEvent(
                PointerUpEvt(
                    e.buttons.toInt(),
                    shiftKeyDown = e.shiftKey,
                    movedSinceLastPointerDown,
                    pointerDevicePosition,
                )
            )
        } finally {
            onPointerUpListener?.dispose()
            onPointerUpListener = null

            // Stop listening to window move events and start listening to canvas move events again.
            onPointerMoveListener?.dispose()
            onPointerMoveListener =
                disposableListener(renderContext.canvas, "pointermove", ::onPointerMove)
        }
    }

    private fun onPointerMove(e: PointerEvent) {
        processPointerEvent(e)

        state = state.processEvent(
            PointerMoveEvt(
                e.buttons.toInt(),
                shiftKeyDown = e.shiftKey,
                movedSinceLastPointerDown,
                pointerDevicePosition,
            )
        )
    }

    private fun processPointerEvent(e: PointerEvent) {
        val rect = renderContext.canvas.getBoundingClientRect()
        pointerPosition.set(e.clientX - rect.left, e.clientY - rect.top)
        pointerDevicePosition.copy(pointerPosition)
        renderContext.pointerPosToDeviceCoords(pointerDevicePosition)

        when (e.type) {
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

    private fun returnToIdleState() {
        if (state !is IdleState) {
            state.cancel()
            state = IdleState(stateContext, entityManipulationEnabled)
        }
    }
}
