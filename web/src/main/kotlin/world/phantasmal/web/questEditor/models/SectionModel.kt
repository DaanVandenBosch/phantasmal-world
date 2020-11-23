package world.phantasmal.web.questEditor.models

import world.phantasmal.web.core.toEuler
import world.phantasmal.web.core.toQuaternion
import world.phantasmal.web.externals.three.Euler
import world.phantasmal.web.externals.three.Vector3

class SectionModel(
    val id: Int,
    val position: Vector3,
    val rotation: Euler,
    val areaVariant: AreaVariantModel,
) {
    init {
        require(id >= -1) {
            "id should be greater than or equal to -1 but was $id."
        }
    }

    val inverseRotation: Euler = rotation.toQuaternion().inverse().toEuler()
}
