@file:JsModule("@babylonjs/core")
@file:JsNonModule

package world.phantasmal.web.externals

import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement

external class Vector3(x: Double, y: Double, z: Double) {
    var x: Double
    var y: Double
    var z: Double

    fun toQuaternion(): Quaternion

    fun addInPlace(otherVector: Vector3): Vector3

    fun addInPlaceFromFloats(x: Double, y: Double, z: Double): Vector3

    companion object {
        fun Zero(): Vector3
    }
}

external class Quaternion

open external class ThinEngine {
    val description: String

    /**
     * Register and execute a render loop. The engine can have more than one render function
     * @param renderFunction defines the function to continuously execute
     */
    fun runRenderLoop(renderFunction: () -> Unit)
}

external class Engine(
    canvasOrContext: HTMLCanvasElement?,
    antialias: Boolean = definedExternally,
) : ThinEngine

external class Scene(engine: Engine) {
    fun render()
}

open external class Node {
    /**
     * Releases resources associated with this node.
     * @param doNotRecurse Set to true to not recurse into each children (recurse into each children by default)
     * @param disposeMaterialAndTextures Set to true to also dispose referenced materials and textures (false by default)
     */
    fun dispose(
        doNotRecurse: Boolean = definedExternally,
        disposeMaterialAndTextures: Boolean = definedExternally,
    )
}

open external class Camera : Node {
    fun attachControl(element: HTMLElement, noPreventDefault: Boolean = definedExternally)
}

open external class TargetCamera : Camera

/**
 * @param setActiveOnSceneIfNoneActive default true
 */
external class ArcRotateCamera(
    name: String,
    alpha: Double,
    beta: Double,
    radius: Double,
    target: Vector3,
    scene: Scene,
    setActiveOnSceneIfNoneActive: Boolean = definedExternally,
) : TargetCamera

abstract external class Light : Node

external class HemisphericLight(name: String, direction: Vector3, scene: Scene) : Light

open external class TransformNode : Node

abstract external class AbstractMesh : TransformNode

external class Mesh : AbstractMesh

external class MeshBuilder {
    companion object {
        interface CreateCylinderOptions {
            var height: Double
            var diameterTop: Double
            var diameterBottom: Double
            var diameter: Double
            var tessellation: Double
            var subdivisions: Double
            var arc: Double
        }

        fun CreateCylinder(
            name: String,
            options: CreateCylinderOptions,
            scene: Scene? = definedExternally,
        ): Mesh
    }
}
