@file:JsModule("three")
@file:JsNonModule
@file:Suppress("unused", "ClassName", "CovariantEquals")

package world.phantasmal.web.externals.three

import org.khronos.webgl.Float32Array
import org.khronos.webgl.Uint16Array
import org.w3c.dom.HTMLCanvasElement

external interface Vector

external class Vector2(x: Double = definedExternally, y: Double = definedExternally) : Vector {
    var x: Double
    var y: Double

    /**
     * Sets value of this vector.
     */
    fun set(x: Double, y: Double): Vector2

    /**
     * Copies value of v to this vector.
     */
    fun copy(v: Vector2): Vector2

    /**
     * Checks for strict equality of this vector and v.
     */
    fun equals(v: Vector2): Boolean
}

external class Vector3(
    x: Double = definedExternally,
    y: Double = definedExternally,
    z: Double = definedExternally,
) : Vector {
    var x: Double
    var y: Double
    var z: Double

    /**
     * Sets value of this vector.
     */
    fun set(x: Double, y: Double, z: Double): Vector3

    fun clone(): Vector3

    /**
     * Copies value of v to this vector.
     */
    fun copy(v: Vector3): Vector3

    /**
     * Checks for strict equality of this vector and v.
     */
    fun equals(v: Vector3): Boolean

    /**
     * Adds [v] to this vector.
     */
    fun add(v: Vector3): Vector3

    /**
     * Subtracts [v] from this vector.
     */
    fun sub(v: Vector3): Vector3

    /**
     * Sets this vector to a - b.
     */
    fun subVectors(a: Vector3, b: Vector3): Vector3

    /**
     * Multiplies this vector by scalar s.
     */
    fun multiplyScalar(s: Double): Vector3

    /**
     * Inverts this vector.
     */
    fun negate(): Vector3

    /**
     * Computes dot product of this vector and v.
     */
    fun dot(v: Vector3): Double

    fun length(): Double

    fun normalize(): Vector3

    /**
     * Sets this vector to cross product of itself and [v].
     */
    fun cross(v: Vector3): Vector3

    fun distanceTo(v: Vector3): Double

    fun applyEuler(euler: Euler): Vector3
    fun applyMatrix3(m: Matrix3): Vector3
    fun applyNormalMatrix(m: Matrix3): Vector3
    fun applyMatrix4(m: Matrix4): Vector3
    fun applyQuaternion(q: Quaternion): Vector3
}

external class Quaternion(
    x: Double = definedExternally,
    y: Double = definedExternally,
    z: Double = definedExternally,
    w: Double = definedExternally,
) {
    fun setFromEuler(euler: Euler): Quaternion

    /**
     * Inverts this quaternion.
     */
    fun inverse(): Quaternion

    /**
     * Multiplies this quaternion by [q].
     */
    fun multiply(q: Quaternion): Quaternion
}

external class Euler(
    x: Double = definedExternally,
    y: Double = definedExternally,
    z: Double = definedExternally,
    order: String = definedExternally,
) {
    var x: Double
    var y: Double
    var z: Double

    fun set(x: Double, y: Double, z: Double, order: String = definedExternally): Euler
    fun copy(euler: Euler): Euler
    fun setFromQuaternion(q: Quaternion, order: String = definedExternally): Euler
}

external class Matrix3 {
    fun getNormalMatrix(matrix4: Matrix4): Matrix3
}

external class Matrix4 {
    fun compose(translation: Vector3, rotation: Quaternion, scale: Vector3): Matrix4

    fun premultiply(m: Matrix4): Matrix4
}

external class Ray(origin: Vector3 = definedExternally, direction: Vector3 = definedExternally) {
    var origin: Vector3
    var direction: Vector3

    fun intersectPlane(plane: Plane, target: Vector3): Vector3?
}

external class Face3(
    a: Int,
    b: Int,
    c: Int,
    normal: Vector3 = definedExternally,
    color: Color = definedExternally,
    materialIndex: Int = definedExternally,
) {
    var normal: Vector3
}

external class Plane(normal: Vector3 = definedExternally, constant: Double = definedExternally) {
    fun set(normal: Vector3, constant: Double): Plane
    fun setFromNormalAndCoplanarPoint(normal: Vector3, point: Vector3): Plane
    fun projectPoint(point: Vector3, target: Vector3): Vector3
}

open external class EventDispatcher

external interface Renderer {
    val domElement: HTMLCanvasElement

    fun render(scene: Object3D, camera: Camera)

    fun setSize(width: Double, height: Double)
}

external interface WebGLRendererParameters {
    /**
     * A Canvas where the renderer draws its output.
     */
    var canvas: HTMLCanvasElement /* HTMLCanvasElement | OffscreenCanvas */
    var alpha: Boolean
    var premultipliedAlpha: Boolean
    var antialias: Boolean
}

external class WebGLRenderer(parameters: WebGLRendererParameters = definedExternally) : Renderer {
    override val domElement: HTMLCanvasElement

    override fun render(scene: Object3D, camera: Camera)

    override fun setSize(width: Double, height: Double)

    fun setPixelRatio(value: Double)

    fun dispose()
}

open external class Object3D {
    /**
     * Optional name of the object (doesn't need to be unique).
     */
    var name: String

    var parent: Object3D?

    var children: Array<Object3D>

    /**
     * Object's local position.
     */
    val position: Vector3

    /**
     * Object's local rotation (Euler angles), in radians.
     */
    val rotation: Euler

    /**
     * Global rotation.
     */
    val quaternion: Quaternion

    /**
     * Object's local scale.
     */
    val scale: Vector3

    /**
     * Local transform.
     */
    var matrix: Matrix4

    var visible: Boolean

    var renderOrder: Int

    /**
     * An object that can be used to store custom data about the Object3d. It should not hold references to functions as these will not be cloned.
     */
    var userData: Any

    fun add(vararg `object`: Object3D): Object3D
    fun remove(vararg `object`: Object3D): Object3D
    fun clear(): Object3D

    /**
     * Updates local transform.
     */
    fun updateMatrix()

    /**
     * Updates global transform of the object and its children.
     */
    fun updateMatrixWorld(force: Boolean = definedExternally)

    fun clone(recursive: Boolean = definedExternally): Object3D
}

external class Group : Object3D

open external class Mesh(
    geometry: Geometry = definedExternally,
    material: Material = definedExternally,
) : Object3D {
    constructor(
        geometry: Geometry,
        material: Array<Material>,
    )

    constructor(
        geometry: BufferGeometry = definedExternally,
        material: Material = definedExternally,
    )

    constructor(
        geometry: BufferGeometry,
        material: Array<Material>,
    )

    var geometry: Any /* Geometry | BufferGeometry */
    var material: Any /* Material | Material[] */

    fun translateY(distance: Double): Mesh
}

external class InstancedMesh(
    geometry: Geometry,
    material: Material,
    count: Int,
) : Mesh {
    constructor(
        geometry: Geometry,
        material: Array<Material>,
        count: Int,
    )

    constructor(
        geometry: BufferGeometry,
        material: Material,
        count: Int,
    )

    constructor(
        geometry: BufferGeometry,
        material: Array<Material>,
        count: Int,
    )

    var count: Int
    var instanceMatrix: BufferAttribute

    fun getMatrixAt(index: Int, matrix: Matrix4)
    fun setMatrixAt(index: Int, matrix: Matrix4)
}

open external class Line : Object3D

open external class LineSegments : Line

open external class BoxHelper(
    `object`: Object3D = definedExternally,
    color: Color = definedExternally,
) : LineSegments {
    fun update(`object`: Object3D = definedExternally)

    fun setFromObject(`object`: Object3D): BoxHelper
}

external class Scene : Object3D {
    var background: dynamic /* null | Color | Texture | WebGLCubeRenderTarget */
}

open external class Camera : Object3D {
    fun getWorldDirection(target: Vector3): Vector3
}

external class PerspectiveCamera(
    fov: Double = definedExternally,
    aspect: Double = definedExternally,
    near: Double = definedExternally,
    far: Double = definedExternally,
) : Camera {
    var fov: Double
    var aspect: Double
    var near: Double
    var far: Double

    /**
     * Updates the camera projection matrix. Must be called after change of parameters.
     */
    fun updateProjectionMatrix()
}

external class OrthographicCamera(
    left: Double,
    right: Double,
    top: Double,
    bottom: Double,
    near: Double = definedExternally,
    far: Double = definedExternally,
) : Camera {
    /**
     * Camera frustum left plane.
     */
    var left: Double

    /**
     * Camera frustum right plane.
     */
    var right: Double

    /**
     * Camera frustum top plane.
     */
    var top: Double

    /**
     * Camera frustum bottom plane.
     */
    var bottom: Double

    /**
     * Camera frustum near plane.
     */
    var near: Double

    /**
     * Camera frustum far plane.
     */
    var far: Double

    /**
     * Updates the camera projection matrix. Must be called after change of parameters.
     */
    fun updateProjectionMatrix()
}

open external class Light : Object3D

external class HemisphereLight(
    skyColor: Color = definedExternally,
    groundColor: Color = definedExternally,
    intensity: Double = definedExternally,
) : Light {
    constructor(
        skyColor: Int = definedExternally,
        groundColor: Int = definedExternally,
        intensity: Double = definedExternally,
    )

    constructor(
        skyColor: String = definedExternally,
        groundColor: String = definedExternally,
        intensity: Double = definedExternally,
    )
}

external class Color() {
    constructor(r: Double, g: Double, b: Double)
    constructor(color: Color)
    constructor(color: String)
    constructor(color: Int)

    fun setHSL(h: Double, s: Double, l: Double): Color
}

open external class Geometry : EventDispatcher {
    /**
     * The array of vertices hold every position of points of the model.
     * To signal an update in this array, Geometry.verticesNeedUpdate needs to be set to true.
     */
    var vertices: Array<Vector3>

    /**
     * Array of triangles or/and quads.
     * The array of faces describe how each vertex in the model is connected with each other.
     * To signal an update in this array, Geometry.elementsNeedUpdate needs to be set to true.
     */
    var faces: Array<Face3>

    /**
     * Array of face UV layers.
     * Each UV layer is an array of UV matching order and number of vertices in faces.
     * To signal an update in this array, Geometry.uvsNeedUpdate needs to be set to true.
     */
    var faceVertexUvs: Array<Array<Array<Vector2>>>

    fun translate(x: Double, y: Double, z: Double): Geometry

    /**
     * Computes bounding box of the geometry, updating {@link Geometry.boundingBox} attribute.
     */
    fun computeBoundingBox()

    /**
     * Computes bounding sphere of the geometry, updating Geometry.boundingSphere attribute.
     * Neither bounding boxes or bounding spheres are computed by default. They need to be explicitly computed, otherwise they are null.
     */
    fun computeBoundingSphere()

    fun dispose()
}

external class PlaneGeometry(
    width: Double = definedExternally,
    height: Double = definedExternally,
    widthSegments: Double = definedExternally,
    heightSegments: Double = definedExternally,
) : Geometry

open external class BufferGeometry : EventDispatcher {
    var boundingBox: Box3?

    fun setIndex(index: BufferAttribute?)
    fun setIndex(index: Array<Double>?)

    fun setAttribute(name: String, attribute: BufferAttribute): BufferGeometry
    fun setAttribute(name: String, attribute: InterleavedBufferAttribute): BufferGeometry

    fun addGroup(start: Int, count: Int, materialIndex: Int = definedExternally)

    fun translate(x: Double, y: Double, z: Double): BufferGeometry

    fun computeBoundingBox()
    fun computeBoundingSphere()

    fun dispose()
}

external class CylinderBufferGeometry(
    radiusTop: Double = definedExternally,
    radiusBottom: Double = definedExternally,
    height: Double = definedExternally,
    radialSegments: Int = definedExternally,
    heightSegments: Int = definedExternally,
    openEnded: Boolean = definedExternally,
    thetaStart: Double = definedExternally,
    thetaLength: Double = definedExternally,
) : BufferGeometry

open external class BufferAttribute {
    var needsUpdate: Boolean

    fun copyAt(index1: Int, attribute: BufferAttribute, index2: Int): BufferAttribute
}

external class Uint16BufferAttribute(
    array: Uint16Array,
    itemSize: Int,
    normalize: Boolean = definedExternally,
) : BufferAttribute

external class Float32BufferAttribute(
    array: Float32Array,
    itemSize: Int,
    normalize: Boolean = definedExternally,
) : BufferAttribute

external class InterleavedBufferAttribute

external interface Side
external object FrontSide : Side
external object BackSide : Side
external object DoubleSide : Side

external interface Blending
external object NoBlending : Blending
external object NormalBlending : Blending
external object AdditiveBlending : Blending
external object SubtractiveBlending : Blending
external object MultiplyBlending : Blending
external object CustomBlending : Blending

external interface MaterialParameters {
    var alphaTest: Double
    var blending: Blending
    var side: Side
    var transparent: Boolean
}

open external class Material : EventDispatcher {
    /**
     * This disposes the material. Textures of a material don't get disposed. These needs to be disposed by [Texture].
     */
    fun dispose()
}

external interface MeshBasicMaterialParameters : MaterialParameters {
    var color: Color
    var opacity: Double
    var map: Texture?
    var wireframe: Boolean
    var wireframeLinewidth: Double
    var skinning: Boolean
}

external class MeshBasicMaterial(
    parameters: MeshBasicMaterialParameters = definedExternally,
) : Material {
    var map: Texture?
}

external interface MeshLambertMaterialParameters : MaterialParameters {
    var color: Color
    var skinning: Boolean
}

external class MeshLambertMaterial(
    parameters: MeshLambertMaterialParameters = definedExternally,
) : Material

open external class Texture : EventDispatcher {
    var needsUpdate: Boolean

    fun dispose()
}

external interface Mapping
external object UVMapping : Mapping
external object CubeReflectionMapping : Mapping
external object CubeRefractionMapping : Mapping
external object EquirectangularReflectionMapping : Mapping
external object EquirectangularRefractionMapping : Mapping
external object CubeUVReflectionMapping : Mapping
external object CubeUVRefractionMapping : Mapping

external interface Wrapping
external object RepeatWrapping : Wrapping
external object ClampToEdgeWrapping : Wrapping
external object MirroredRepeatWrapping : Wrapping

external interface TextureFilter
external object NearestFilter : TextureFilter
external object NearestMipmapNearestFilter : TextureFilter
external object NearestMipMapNearestFilter : TextureFilter
external object NearestMipmapLinearFilter : TextureFilter
external object NearestMipMapLinearFilter : TextureFilter
external object LinearFilter : TextureFilter
external object LinearMipmapNearestFilter : TextureFilter
external object LinearMipMapNearestFilter : TextureFilter
external object LinearMipmapLinearFilter : TextureFilter
external object LinearMipMapLinearFilter : TextureFilter

external interface TextureDataType
external object UnsignedByteType : TextureDataType
external object ByteType : TextureDataType
external object ShortType : TextureDataType
external object UnsignedShortType : TextureDataType
external object IntType : TextureDataType
external object UnsignedIntType : TextureDataType
external object FloatType : TextureDataType
external object HalfFloatType : TextureDataType
external object UnsignedShort4444Type : TextureDataType
external object UnsignedShort5551Type : TextureDataType
external object UnsignedShort565Type : TextureDataType
external object UnsignedInt248Type : TextureDataType

// DDS / ST3C Compressed texture formats
external interface CompressedPixelFormat
external object RGB_S3TC_DXT1_Format : CompressedPixelFormat
external object RGBA_S3TC_DXT1_Format : CompressedPixelFormat
external object RGBA_S3TC_DXT3_Format : CompressedPixelFormat
external object RGBA_S3TC_DXT5_Format : CompressedPixelFormat

external interface TextureEncoding
external object LinearEncoding : TextureEncoding
external object sRGBEncoding : TextureEncoding
external object GammaEncoding : TextureEncoding
external object RGBEEncoding : TextureEncoding
external object LogLuvEncoding : TextureEncoding
external object RGBM7Encoding : TextureEncoding
external object RGBM16Encoding : TextureEncoding
external object RGBDEncoding : TextureEncoding

external class CompressedTexture(
    mipmaps: Array<dynamic>, /* Should have data, height and width. */
    width: Int,
    height: Int,
    format: CompressedPixelFormat = definedExternally,
    type: TextureDataType = definedExternally,
    mapping: Mapping = definedExternally,
    wrapS: Wrapping = definedExternally,
    wrapT: Wrapping = definedExternally,
    magFilter: TextureFilter = definedExternally,
    minFilter: TextureFilter = definedExternally,
    anisotropy: Double = definedExternally,
    encoding: TextureEncoding = definedExternally,
) : Texture

external class Box3(min: Vector3 = definedExternally, max: Vector3 = definedExternally) {
    var min: Vector3
    var max: Vector3
}

external enum class MOUSE {
    LEFT,
    MIDDLE,
    RIGHT,
    ROTATE,
    DOLLY,
    PAN,
}

external class Raycaster(
    origin: Vector3 = definedExternally,
    direction: Vector3 = definedExternally,
    near: Double = definedExternally,
    far: Double = definedExternally,
) {
    var ray: Ray

    fun set(origin: Vector3, direction: Vector3)

    /**
     * Updates the ray with a new origin and direction.
     * @param coords 2D coordinates of the mouse, in normalized device coordinates (NDC)---X and Y components should be between -1 and 1.
     * @param camera camera from which the ray should originate
     */
    fun setFromCamera(coords: Vector2, camera: Camera)

    fun intersectObject(
        `object`: Object3D,
        recursive: Boolean = definedExternally,
        optionalTarget: Array<Intersection> = definedExternally,
    ): Array<Intersection>
}

external interface Intersection {
    var distance: Double
    var distanceToRay: Double?
    var point: Vector3
    var index: Double?
    var face: Face3?
    var faceIndex: Int?
    var `object`: Object3D
    var uv: Vector2?
    var instanceId: Int?
}
