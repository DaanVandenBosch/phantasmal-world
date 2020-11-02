@file:JsModule("@babylonjs/core")
@file:JsNonModule
@file:Suppress("FunctionName", "unused")

package world.phantasmal.web.externals.babylon

import org.khronos.webgl.Float32Array
import org.khronos.webgl.Uint16Array
import org.w3c.dom.HTMLCanvasElement

external class Vector2(x: Double, y: Double) {
    var x: Double
    var y: Double

    fun addInPlace(otherVector: Vector2): Vector2

    fun addInPlaceFromFloats(x: Double, y: Double): Vector2

    fun copyFrom(source: Vector2): Vector2

    companion object {
        fun Zero(): Vector2
    }
}

external class Vector3(x: Double, y: Double, z: Double) {
    var x: Double
    var y: Double
    var z: Double

    fun toQuaternion(): Quaternion

    fun addInPlace(otherVector: Vector3): Vector3

    fun addInPlaceFromFloats(x: Double, y: Double, z: Double): Vector3

    fun copyFrom(source: Vector3): Vector3

    companion object {
        fun One(): Vector3
        fun Up(): Vector3
        fun Zero(): Vector3
        fun TransformCoordinates(vector: Vector3, transformation: Matrix): Vector3
        fun TransformCoordinatesToRef(vector: Vector3, transformation: Matrix, result: Vector3)
        fun TransformNormal(vector: Vector3, transformation: Matrix): Vector3
        fun TransformNormalToRef(vector: Vector3, transformation: Matrix, result: Vector3)
    }
}

external class Quaternion(
    x: Double = definedExternally,
    y: Double = definedExternally,
    z: Double = definedExternally,
    w: Double = definedExternally,
) {
    /**
     * Multiplies two quaternions
     * @return a new quaternion set as the multiplication result of the current one with the given one "q1"
     */
    fun multiply(q1: Quaternion): Quaternion

    /**
     * Updates the current quaternion with the multiplication of itself with the given one "q1"
     * @return the current, updated quaternion
     */
    fun multiplyInPlace(q1: Quaternion): Quaternion

    /**
     * Sets the given "result" as the the multiplication result of the current one with the given one "q1"
     * @return the current quaternion
     */
    fun multiplyToRef(q1: Quaternion, result: Quaternion): Quaternion

    companion object {
        fun Identity(): Quaternion
        fun FromEulerAngles(x: Double, y: Double, z: Double): Quaternion
        fun RotationYawPitchRoll(yaw: Double, pitch: Double, roll: Double): Quaternion
    }
}

external class Matrix {
    fun multiply(other: Matrix): Matrix
    fun multiplyToRef(other: Matrix, result: Matrix): Matrix
    fun toNormalMatrix(ref: Matrix)

    companion object {
        fun Identity(): Matrix
        fun Compose(scale: Vector3, rotation: Quaternion, translation: Vector3): Matrix
    }
}

open external class ThinEngine {
    val description: String

    /**
     * Register and execute a render loop. The engine can have more than one render function
     * @param renderFunction defines the function to continuously execute
     */
    fun runRenderLoop(renderFunction: () -> Unit)

    fun dispose()
}

external class Engine(
    canvasOrContext: HTMLCanvasElement?,
    antialias: Boolean = definedExternally,
) : ThinEngine

external class Scene(engine: Engine) {
    fun render()
    fun addLight(light: Light)
    fun addMesh(newMesh: AbstractMesh, recursive: Boolean? = definedExternally)
    fun addTransformNode(newTransformNode: TransformNode)
    fun removeLight(toRemove: Light)
    fun removeMesh(toRemove: TransformNode, recursive: Boolean? = definedExternally)
    fun removeTransformNode(toRemove: TransformNode)
    fun dispose()
}

open external class Node {
    var metadata: Any?
    var parent: Node?
    var position: Vector3
    var rotation: Vector3
    var scaling: Vector3

    fun setEnabled(value: Boolean)

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
    fun attachControl(noPreventDefault: Boolean = definedExternally)
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
) : TargetCamera {
    var inertia: Double
    var angularSensibilityX: Double
    var angularSensibilityY: Double
    var panningInertia: Double
    var panningSensibility: Double
    var panningAxis: Vector3
    var pinchDeltaPercentage: Double
    var wheelDeltaPercentage: Double

    fun attachControl(
        element: HTMLCanvasElement,
        noPreventDefault: Boolean,
        useCtrlForPanning: Boolean,
        panningMouseButton: Int,
    )
}

abstract external class Light : Node

external class HemisphericLight(name: String, direction: Vector3, scene: Scene) : Light

open external class TransformNode(
    name: String,
    scene: Scene? = definedExternally,
    isPure: Boolean = definedExternally,
) : Node {
}

abstract external class AbstractMesh : TransformNode

external class Mesh(
    name: String,
    scene: Scene? = definedExternally,
    parent: Node? = definedExternally,
    source: Mesh? = definedExternally,
    doNotCloneChildren: Boolean = definedExternally,
    clonePhysicsImpostor: Boolean = definedExternally,
) : AbstractMesh {
    fun createInstance(name: String): InstancedMesh
}

external class InstancedMesh : AbstractMesh

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

external class VertexData {
    var positions: Float32Array? // number[] | Float32Array
    var normals: Float32Array? // number[] | Float32Array
    var uvs: Float32Array? // number[] | Float32Array
    var indices: Uint16Array? // number[] | Int32Array | Uint32Array | Uint16Array

    fun applyToMesh(mesh: Mesh, updatable: Boolean = definedExternally): VertexData
}
