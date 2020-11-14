@file:JsModule("@babylonjs/core")
@file:JsNonModule
@file:Suppress("FunctionName", "unused", "CovariantEquals")

package world.phantasmal.web.externals.babylon

import org.khronos.webgl.Float32Array
import org.khronos.webgl.Uint16Array
import org.w3c.dom.HTMLCanvasElement

external class Vector2(x: Double, y: Double) {
    var x: Double
    var y: Double

    fun set(x: Double, y: Double): Vector2
    fun addInPlace(otherVector: Vector2): Vector2
    fun addInPlaceFromFloats(x: Double, y: Double): Vector2
    fun subtract(otherVector: Vector2): Vector2
    fun negate(): Vector2
    fun negateInPlace(): Vector2
    fun clone(): Vector2
    fun copyFrom(source: Vector2): Vector2
    fun equals(otherVector: Vector2): Boolean

    companion object {
        fun Zero(): Vector2
        fun Dot(left: Vector2, right: Vector2): Double
    }
}

external class Vector3(x: Double, y: Double, z: Double) {
    var x: Double
    var y: Double
    var z: Double

    fun set(x: Double, y: Double, z: Double): Vector2
    fun toQuaternion(): Quaternion
    fun add(otherVector: Vector3): Vector3
    fun addInPlace(otherVector: Vector3): Vector3
    fun addInPlaceFromFloats(x: Double, y: Double, z: Double): Vector3
    fun subtract(otherVector: Vector3): Vector3
    fun subtractInPlace(otherVector: Vector3): Vector3
    fun negate(): Vector3
    fun negateInPlace(): Vector3
    fun cross(other: Vector3): Vector3

    /**
     * Returns a new Vector3 set with the current Vector3 coordinates multiplied by the float "scale"
     */
    fun scale(scale: Double): Vector3

    /**
     * Multiplies the Vector3 coordinates by the float "scale"
     *
     * @return the current updated Vector3
     */
    fun scaleInPlace(scale: Double): Vector3

    fun rotateByQuaternionToRef(quaternion: Quaternion, result: Vector3): Vector3
    fun clone(): Vector3
    fun copyFrom(source: Vector3): Vector3
    fun equals(otherVector: Vector3): Boolean

    companion object {
        fun One(): Vector3
        fun Up(): Vector3
        fun Down(): Vector3
        fun Zero(): Vector3
        fun Dot(left: Vector3, right: Vector3): Double
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
    fun toEulerAngles(): Vector3
    fun toEulerAnglesToRef(result: Vector3): Quaternion
    fun rotateByQuaternionToRef(quaternion: Quaternion, result: Vector3): Vector3
    fun clone(): Quaternion
    fun copyFrom(other: Quaternion): Quaternion

    companion object {
        fun Identity(): Quaternion
        fun FromEulerAngles(x: Double, y: Double, z: Double): Quaternion
        fun FromEulerAnglesToRef(x: Double, y: Double, z: Double, result: Quaternion): Quaternion
        fun RotationYawPitchRoll(yaw: Double, pitch: Double, roll: Double): Quaternion
        fun Inverse(q: Quaternion): Quaternion
    }
}

external class Matrix {
    fun multiply(other: Matrix): Matrix
    fun multiplyToRef(other: Matrix, result: Matrix): Matrix
    fun toNormalMatrix(ref: Matrix)
    fun copyFrom(other: Matrix): Matrix
    fun equals(value: Matrix): Boolean

    companion object {
        val IdentityReadOnly: Matrix

        fun Identity(): Matrix
        fun Compose(scale: Vector3, rotation: Quaternion, translation: Vector3): Matrix
    }
}

external class EventState

external class Observable<T> {
    fun add(
        callback: (eventData: T, eventState: EventState) -> Unit,
        mask: Int = definedExternally,
        insertFirst: Boolean = definedExternally,
        scope: Any = definedExternally,
        unregisterOnFirstCall: Boolean = definedExternally,
    ): Observer<T>?

    fun remove(observer: Observer<T>?): Boolean

    fun removeCallback(
        callback: (eventData: T, eventState: EventState) -> Unit,
        scope: Any = definedExternally,
    ): Boolean
}

external class Observer<T>

open external class ThinEngine {
    val description: String

    /**
     * Register and execute a render loop. The engine can have more than one render function
     * @param renderFunction defines the function to continuously execute
     */
    fun runRenderLoop(renderFunction: () -> Unit)

    /**
     * stop executing a render loop function and remove it from the execution array
     * @param renderFunction defines the function to be removed. If not provided all functions will
     * be removed.
     */
    fun stopRenderLoop(renderFunction: () -> Unit = definedExternally)
    fun getRenderWidth(useScreen: Boolean = definedExternally): Double
    fun getRenderHeight(useScreen: Boolean = definedExternally): Double

    fun dispose()
}

open external class Engine(
    canvasOrContext: HTMLCanvasElement?,
    antialias: Boolean = definedExternally,
) : ThinEngine

external class NullEngine : Engine

external class Ray(origin: Vector3, direction: Vector3, length: Double = definedExternally) {
    var origin: Vector3
    var direction: Vector3
    var length: Double

    fun intersectsPlane(plane: Plane): Double?

    companion object {
        fun Zero(): Ray
    }
}

external class PickingInfo {
    val bu: Double
    val bv: Double
    val distance: Double
    val faceId: Int
    val hit: Boolean
    val originMesh: AbstractMesh?
    val pickedMesh: AbstractMesh?
    val pickedPoint: Vector3?
    val ray: Ray?

    fun getNormal(
        useWorldCoordinates: Boolean = definedExternally,
        useVerticesNormals: Boolean = definedExternally,
    ): Vector3?

    fun getTextureCoordinates(): Vector2?
}

external class Scene(engine: Engine) {
    var useRightHandedSystem: Boolean
    var clearColor: Color4
    var pointerX: Double
    var pointerY: Double

    fun render()
    fun addLight(light: Light)
    fun addMesh(newMesh: AbstractMesh, recursive: Boolean? = definedExternally)
    fun addTransformNode(newTransformNode: TransformNode)
    fun removeLight(toRemove: Light)
    fun removeMesh(toRemove: TransformNode, recursive: Boolean? = definedExternally)
    fun removeTransformNode(toRemove: TransformNode)

    fun createPickingRay(
        x: Double,
        y: Double,
        world: Matrix,
        camera: Camera?,
        cameraViewSpace: Boolean = definedExternally,
    ): Ray

    fun createPickingRayToRef(
        x: Double,
        y: Double,
        world: Matrix,
        result: Ray,
        camera: Camera?,
        cameraViewSpace: Boolean = definedExternally,
    ): Scene

    fun createPickingRayInCameraSpaceToRef(
        x: Double,
        y: Double,
        result: Ray,
        camera: Camera = definedExternally,
    ): Scene

    fun pick(
        x: Double,
        y: Double,
        predicate: (AbstractMesh) -> Boolean = definedExternally,
        fastCheck: Boolean = definedExternally,
        camera: Camera? = definedExternally,
        trianglePredicate: (p0: Vector3, p1: Vector3, p2: Vector3, ray: Ray) -> Boolean = definedExternally,
    ): PickingInfo?

    fun pickWithRay(
        ray: Ray,
        predicate: (AbstractMesh) -> Boolean = definedExternally,
        fastCheck: Boolean = definedExternally,
        trianglePredicate: (p0: Vector3, p1: Vector3, p2: Vector3, ray: Ray) -> Boolean = definedExternally,
    ): PickingInfo?

    /**
     * @param x X position on screen
     * @param y Y position on screen
     * @param predicate Predicate function used to determine eligible meshes. Can be set to null. In this case, a mesh must be enabled, visible and with isPickable set to true
     */
    fun multiPick(
        x: Double,
        y: Double,
        predicate: (AbstractMesh) -> Boolean = definedExternally,
        camera: Camera = definedExternally,
        trianglePredicate: (p0: Vector3, p1: Vector3, p2: Vector3, ray: Ray) -> Boolean = definedExternally,
    ): Array<PickingInfo>?

    fun multiPickWithRay(
        ray: Ray,
        predicate: (AbstractMesh) -> Boolean = definedExternally,
        trianglePredicate: (p0: Vector3, p1: Vector3, p2: Vector3, ray: Ray) -> Boolean = definedExternally,
    ): Array<PickingInfo>?

    fun dispose()
}

open external class Node {
    var metadata: Any?
    var parent: Node?

    fun isEnabled(checkAncestors: Boolean = definedExternally): Boolean
    fun setEnabled(value: Boolean)
    fun getWorldMatrix(): Matrix

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
    var minZ: Double
    var maxZ: Double
    val absoluteRotation: Quaternion
    val onProjectionMatrixChangedObservable: Observable<Camera>
    val onViewMatrixChangedObservable: Observable<Camera>
    val onAfterCheckInputsObservable: Observable<Camera>

    fun getViewMatrix(force: Boolean = definedExternally): Matrix
    fun getProjectionMatrix(force: Boolean = definedExternally): Matrix
    fun getTransformationMatrix(): Matrix
    fun attachControl(noPreventDefault: Boolean = definedExternally)
    fun detachControl()
    fun storeState(): Camera
    fun restoreState(): Boolean
}

open external class TargetCamera : Camera {
    var target: Vector3
}

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
    var alpha: Double
    var beta: Double
    var radius: Double
    var inertia: Double
    var angularSensibilityX: Double
    var angularSensibilityY: Double
    var panningInertia: Double
    var panningSensibility: Double
    var panningAxis: Vector3
    var pinchDeltaPercentage: Double
    var wheelDeltaPercentage: Double
    var lowerBetaLimit: Double
    val inputs: ArcRotateCameraInputsManager

    fun attachControl(
        element: HTMLCanvasElement,
        noPreventDefault: Boolean,
        useCtrlForPanning: Boolean,
        panningMouseButton: Int,
    )
}

open external class CameraInputsManager<TCamera : Camera> {
    fun attachElement(noPreventDefault: Boolean = definedExternally)
    fun detachElement(disconnect: Boolean = definedExternally)
}

external class ArcRotateCameraInputsManager : CameraInputsManager<ArcRotateCamera>

abstract external class Light : Node

external class HemisphericLight(name: String, direction: Vector3, scene: Scene) : Light {
    var direction: Vector3
}

open external class TransformNode(
    name: String,
    scene: Scene? = definedExternally,
    isPure: Boolean = definedExternally,
) : Node {
    var position: Vector3
    var rotation: Vector3
    var rotationQuaternion: Quaternion?
    val absoluteRotation: Quaternion
    var scaling: Vector3

    fun locallyTranslate(vector3: Vector3): TransformNode
}

abstract external class AbstractMesh : TransformNode {
    var showBoundingBox: Boolean

    fun getBoundingInfo(): BoundingInfo
}

external class Mesh(
    name: String,
    scene: Scene? = definedExternally,
    parent: Node? = definedExternally,
    source: Mesh? = definedExternally,
    doNotCloneChildren: Boolean = definedExternally,
    clonePhysicsImpostor: Boolean = definedExternally,
) : AbstractMesh {
    fun createInstance(name: String): InstancedMesh
    fun bakeCurrentTransformIntoVertices(
        bakeIndependenlyOfChildren: Boolean = definedExternally,
    ): Mesh
}

external class InstancedMesh : AbstractMesh

external class BoundingInfo {
    val boundingBox: BoundingBox
    val boundingSphere: BoundingSphere
}

external class BoundingBox {
    val center: Vector3
    val centerWorld: Vector3
    val directions: Array<Vector3>
    val extendSize: Vector3
    val extendSizeWorld: Vector3
    val maximum: Vector3
    val maximumWorld: Vector3
    val minimum: Vector3
    val minimumWorld: Vector3
    val vectors: Array<Vector3>
    val vectorsWorld: Array<Vector3>
}

external class BoundingSphere {
    val center: Vector3
    val centerWorld: Vector3
    val maximum: Vector3
    val minimum: Vector3
    val radius: Double
    val radiusWorld: Double
}

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

external class Plane(a: Double, b: Double, c: Double, d: Double) {
    var normal: Vector3
    var d: Double

    companion object {
        /**
         *	Note : the vector "normal" is updated because normalized.
         */
        fun FromPositionAndNormal(origin: Vector3, normal: Vector3): Plane
    }
}

external class VertexData {
    var positions: Float32Array? // number[] | Float32Array
    var normals: Float32Array? // number[] | Float32Array
    var uvs: Float32Array? // number[] | Float32Array
    var indices: Uint16Array? // number[] | Int32Array | Uint32Array | Uint16Array

    fun applyToMesh(mesh: Mesh, updatable: Boolean = definedExternally): VertexData
}

external class Color3(
    r: Double = definedExternally,
    g: Double = definedExternally,
    b: Double = definedExternally,
) {
    var r: Double
    var g: Double
    var b: Double
}

external class Color4(
    r: Double = definedExternally,
    g: Double = definedExternally,
    b: Double = definedExternally,
    a: Double = definedExternally,
) {
    var r: Double
    var g: Double
    var b: Double
    var a: Double

    companion object {
        /**
         * Creates a new Color4 from integer values (< 256)
         */
        fun FromInts(r: Int, g: Int, b: Int, a: Int): Color4
    }
}
