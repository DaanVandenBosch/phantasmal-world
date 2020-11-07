package world.phantasmal.web.questEditor.rendering.conversion

import world.phantasmal.lib.fileFormats.CollisionObject
import world.phantasmal.web.core.rendering.conversion.VertexDataBuilder
import world.phantasmal.web.core.rendering.conversion.vec3ToBabylon
import world.phantasmal.web.externals.babylon.Mesh
import world.phantasmal.web.externals.babylon.Scene
import world.phantasmal.web.externals.babylon.TransformNode

fun areaCollisionGeometryToTransformNode(scene: Scene, obj: CollisionObject): TransformNode {
    val node = TransformNode("", scene)

    for (collisionMesh in obj.meshes) {
        val builder = VertexDataBuilder()

        for (triangle in collisionMesh.triangles) {
            val isSectionTransition = (triangle.flags and 0b1000000) != 0
            val isVegetation = (triangle.flags and 0b10000) != 0
            val isGround = (triangle.flags and 0b1) != 0
            val colorIndex = when {
                isSectionTransition -> 3
                isVegetation -> 2
                isGround -> 1
                else -> 0
            }

            // Filter out walls.
            if (colorIndex != 0) {
                val p1 = vec3ToBabylon(collisionMesh.vertices[triangle.index1])
                val p2 = vec3ToBabylon(collisionMesh.vertices[triangle.index2])
                val p3 = vec3ToBabylon(collisionMesh.vertices[triangle.index3])
                val n = vec3ToBabylon(triangle.normal)

                builder.addIndex(builder.vertexCount)
                builder.addVertex(p1, n)
                builder.addIndex(builder.vertexCount)
                builder.addVertex(p3, n)
                builder.addIndex(builder.vertexCount)
                builder.addVertex(p2, n)
            }
        }

        if (builder.vertexCount > 0) {
            val mesh = Mesh("Collision Geometry", scene, parent = node)
            builder.build().applyToMesh(mesh)
        }
    }

    return node
}
