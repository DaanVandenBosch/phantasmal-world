package world.phantasmal.web.core.rendering

import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.core.disposable.TrackedDisposable
import world.phantasmal.web.externals.babylon.Engine
import world.phantasmal.web.externals.babylon.Scene

abstract class Renderer(
    protected val canvas: HTMLCanvasElement,
    protected val engine: Engine,
) : TrackedDisposable() {
    protected val scene = Scene(engine)

    init {
        engine.runRenderLoop {
            scene.render()
        }
    }

    override fun internalDispose() {
        scene.dispose()
        engine.dispose()
        super.internalDispose()
    }

    fun scheduleRender() {
        // TODO: Remove scheduleRender?
    }
}
