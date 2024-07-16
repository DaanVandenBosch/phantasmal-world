@file:JsModule("three/examples/jsm/utils/BufferGeometryUtils")
@file:JsNonModule
@file:Suppress("PropertyName", "unused")

package world.phantasmal.web.externals.three

external object BufferGeometryUtils {
    fun mergeBufferGeometries(
        geometries: Array<BufferGeometry>,
        useGroups: Boolean,
    ): BufferGeometry?
}
