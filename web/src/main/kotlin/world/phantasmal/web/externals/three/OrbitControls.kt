@file:JsModule("three/examples/jsm/controls/OrbitControls")
@file:JsNonModule
@file:Suppress("PropertyName", "unused")

package world.phantasmal.web.externals.three

import org.w3c.dom.HTMLElement

external interface OrbitControlsMouseButtons {
    var LEFT: MOUSE
    var MIDDLE: MOUSE
    var RIGHT: MOUSE
}

external interface OrbitControlsMouseTouches {
    var ONE: TOUCH
    var TWO: TOUCH
}

external class OrbitControls(`object`: Camera, domElement: HTMLElement = definedExternally) {
    var enabled: Boolean
    var enablePan: Boolean
    var enableRotate: Boolean
    var enableZoom: Boolean
    var target: Vector3
    var zoomSpeed: Double
    var screenSpacePanning: Boolean

    var mouseButtons: OrbitControlsMouseButtons

    var touches: OrbitControlsMouseTouches

    fun update(): Boolean

    fun saveState()

    fun reset()

    fun dispose()
}
