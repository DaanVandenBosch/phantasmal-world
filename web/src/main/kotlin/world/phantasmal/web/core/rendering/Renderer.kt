package world.phantasmal.web.core.rendering

import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.core.disposable.Scope
import world.phantasmal.core.disposable.TrackedDisposable
import world.phantasmal.web.externals.Engine
import world.phantasmal.web.externals.Scene

abstract class Renderer(
    scope: Scope,
    protected val canvas: HTMLCanvasElement,
    createEngine: (HTMLCanvasElement) -> Engine,
) : TrackedDisposable(scope) {
    protected val engine = createEngine(canvas)
    protected val scene = Scene(engine)

    init {
        println(engine.description)

        engine.runRenderLoop {
            scene.render()
        }
    }

    override fun internalDispose() {
    }
}
