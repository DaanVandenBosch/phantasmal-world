package world.phantasmal.web.core.rendering

import world.phantasmal.web.externals.three.Object3D

/**
 * Recursively disposes the given object and any geometries/materials/textures/skeletons attached to
 * it and its children.
 */
fun disposeObject3DResources(obj: Object3D) {
    val dynObj = obj.asDynamic()

    dynObj.geometry?.dispose()
    dynObj.skeleton?.dispose()

    if (dynObj.material is Array<*>) {
        for (material in dynObj.material) {
            material.map?.dispose()
            material.dispose()
        }
    } else if (dynObj.material != null) {
        dynObj.material.map?.dispose()
        dynObj.material.dispose()
    }

    if (dynObj.dispose != null) {
        dynObj.dispose()
    }

    for (child in obj.children) {
        disposeObject3DResources(child)
    }
}
