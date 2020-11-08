package world.phantasmal.web.questEditor.models

import world.phantasmal.web.externals.babylon.Quaternion
import world.phantasmal.web.externals.babylon.Vector3

class SectionModel(
    val id: Int,
    val position: Vector3,
    val rotation: Vector3,
    val areaVariant: AreaVariantModel,
) {
    init {
        require(id >= -1) {
            "id should be greater than or equal to -1 but was $id."
        }
    }

    val rotationQuaternion: Quaternion =
        Quaternion.FromEulerAngles(rotation.x, rotation.y, rotation.z)
}
