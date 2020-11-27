package world.phantasmal.web.test

import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.web.externals.three.Camera
import world.phantasmal.web.externals.three.Object3D
import world.phantasmal.web.externals.three.Renderer

class NoopRenderer(override val domElement: HTMLCanvasElement) : Renderer {
    override fun render(scene: Object3D, camera: Camera) {}

    override fun setSize(width: Double, height: Double) {}
}
