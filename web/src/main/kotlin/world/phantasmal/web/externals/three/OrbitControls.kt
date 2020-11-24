@file:JsModule("three/examples/jsm/controls/OrbitControls")
@file:JsNonModule
@file:Suppress("PropertyName")

package world.phantasmal.web.externals.three

import org.w3c.dom.HTMLElement

external interface OrbitControlsMouseButtons {
    var LEFT: MOUSE
    var MIDDLE: MOUSE
    var RIGHT: MOUSE
}

external class OrbitControls(`object`: Camera, domElement: HTMLElement = definedExternally) {
    var enabled: Boolean
    var target: Vector3
    var screenSpacePanning: Boolean

    var mouseButtons: OrbitControlsMouseButtons

    fun update(): Boolean

    fun saveState()

    fun reset()

    fun dispose()
}
