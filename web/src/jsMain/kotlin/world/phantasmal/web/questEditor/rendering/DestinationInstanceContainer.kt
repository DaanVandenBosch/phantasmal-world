package world.phantasmal.web.questEditor.rendering

import world.phantasmal.web.externals.three.*
import world.phantasmal.web.questEditor.models.QuestObjectModel
import world.phantasmal.webui.obj
import kotlin.math.PI

class DestinationInstanceContainer : InstanceContainer<QuestObjectModel, DestinationInstance>(
    InstancedMesh(
        BufferGeometryUtils.mergeBufferGeometries(
            arrayOf(
                SphereGeometry(
                    radius = 4.0,
                    widthSegments = 16,
                    heightSegments = 12,
                ),
                CylinderGeometry(
                    radiusTop = 1.0,
                    radiusBottom = 1.0,
                    height = 10.0,
                    radialSegments = 10,
                ).apply {
                    translate(.0, 5.0, .0)
                },
                ConeGeometry(
                    radius = 3.0,
                    height = 6.0,
                    radialSegments = 20,
                ).apply {
                    translate(.0, 13.0, .0)
                },
            ),
            useGroups = false,
        )!!.apply {
            rotateX(PI / 2)
            computeBoundingBox()
            computeBoundingSphere()
        },
        MeshLambertMaterial(obj { color = COLOR }),
        count = 1000,
    ).apply {
        // Start with 0 instances.
        count = 0
    }
) {
    override fun createInstance(entity: QuestObjectModel, index: Int): DestinationInstance =
        DestinationInstance(entity, mesh, index)

    companion object {
        val COLOR = Color(0x00FFC0)
    }
}
