package world.phantasmal.web.core.rendering

import world.phantasmal.web.externals.three.Object3D

/**
 * Recursively disposes any geometries/materials/textures attached to the given [Object3D] or its
 * children.
 */
fun disposeObject3DResources(obj: Object3D) {
    val dynObj = obj.asDynamic()

    dynObj.geometry?.dispose()

    if (dynObj.material is Array<*>) {
        for (material in dynObj.material) {
            material.map?.dispose()
            material.dispose()
        }
    } else if (dynObj.material != null) {
        dynObj.material.map?.dispose()
        dynObj.material.dispose()
    }

    for (child in obj.children) {
        disposeObject3DResources(child)
    }
}
