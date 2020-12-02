package world.phantasmal.web.test

import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.web.externals.three.Camera
import world.phantasmal.web.externals.three.Color
import world.phantasmal.web.externals.three.Object3D

// WebGLRenderer implementation.
class NopRenderer(val domElement: HTMLCanvasElement) {
    @JsName("render")
    fun render(scene: Object3D, camera: Camera) {
    }

    @JsName("setSize")
    fun setSize(width: Double, height: Double) {
    }

    @JsName("setPixelRatio")
    fun setPixelRatio(value: Double) {
    }

    @JsName("setClearColor")
    fun setClearColor(color: Color) {
    }

    @JsName("clearColor")
    fun clearColor() {
    }

    @JsName("dispose")
    fun dispose() {
    }
}
