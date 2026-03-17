package world.phantasmal.web.questEditor.rendering

import mu.KotlinLogging
import world.phantasmal.cell.observeNow
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.core.disposable.TrackedDisposable
import world.phantasmal.web.core.rendering.disposeObject3DResources
import world.phantasmal.web.externals.three.*
import world.phantasmal.web.questEditor.models.ChallengeMonsterSpawnModel
import world.phantasmal.webui.obj

private val logger = KotlinLogging.logger {}

/**
 * Container for challenge mode random monster spawn point instances.
 * Renders as orange arrows pointing in the spawn rotation direction.
 * These are non-selectable visual indicators.
 */
class ChallengeMonsterSpawnContainer : TrackedDisposable() {
    private val spawnPoints = mutableListOf<SpawnPointInstance>()
    private val disposer = Disposer()

    val mesh: InstancedMesh = InstancedMesh(
        // Create arrow geometry: two thin cylinders forming a ">" shape pointing in +Z direction
        createArrowGeometry(),
        MeshLambertMaterial(obj {
            color = COLOR
            transparent = false
        }),
        count = MAX_INSTANCES,
    ).apply {
        // Start with 0 instances
        count = 0
        frustumCulled = false  // Always render, don't cull
        visible = true
    }

    init {
        mesh.userData = this
    }

    override fun dispose() {
        clearInstances()
        disposeObject3DResources(mesh)
        disposer.dispose()
        super.dispose()
    }

    fun addInstance(model: ChallengeMonsterSpawnModel) {
        if (mesh.count >= MAX_INSTANCES) {
            logger.warn { "InstancedMesh capacity ($MAX_INSTANCES) exceeded, ignoring spawn point." }
            return
        }
        val instanceIndex = mesh.count
        mesh.count++

        val instance = SpawnPointInstance(model, instanceIndex)
        spawnPoints.add(instance)

        // Update matrix immediately
        instance.updateMatrix()
    }

    fun clearInstances() {
        spawnPoints.forEach { it.dispose() }
        spawnPoints.clear()
        mesh.count = 0
    }

    private inner class SpawnPointInstance(
        val model: ChallengeMonsterSpawnModel,
        val instanceIndex: Int,
    ) : TrackedDisposable() {
        private val disposer = Disposer()
        private val helper = Object3D()

        init {
            disposer.add(model.position.observeNow { updateMatrix() })
            disposer.add(model.rotation.observeNow { updateMatrix() })
        }

        fun updateMatrix() {
            val pos = model.position.value
            val rot = model.rotation.value

            helper.position.set(pos.x, pos.y, pos.z)
            helper.rotation.set(rot.x, rot.y, rot.z)
            helper.updateMatrix()

            mesh.setMatrixAt(instanceIndex, helper.matrix)
            mesh.instanceMatrix.needsUpdate = true
        }

        override fun dispose() {
            disposer.dispose()
            super.dispose()
        }
    }

    companion object {
        private const val MAX_INSTANCES = 1000

        // Bright orange color for challenge mode monster visualization
        val COLOR = Color(0xFFB830)

        /**
         * Creates arrow geometry made of two thin cylinders forming a "<" shape.
         * Arrow points in -Z direction (tip at origin).
         */
        private fun createArrowGeometry(): BufferGeometry {
            val arrowLength = 10.0
            val arrowWidth = 6.0
            val lineThickness = 1.5

            // Calculate actual line length using Pythagorean theorem
            val lineLength = kotlin.math.sqrt(arrowWidth * arrowWidth + arrowLength * arrowLength)
            val angle = kotlin.math.atan2(arrowWidth, arrowLength)

            // Left line: from (-arrowWidth, 0, +arrowLength) to (0, 0, 0)
            val leftLine = CylinderGeometry(
                radiusTop = lineThickness,
                radiusBottom = lineThickness,
                height = lineLength,
                radialSegments = 4,
            ).apply {
                // Cylinder is along Y axis by default, rotate to point correctly
                rotateZ(angle)  // Rotate around Z to angle the line
                rotateX(-kotlin.math.PI / 2)  // Rotate to XZ plane
                // Move to midpoint of the line
                translate(-arrowWidth / 2, 0.0, arrowLength / 2)
            }

            // Right line: from (0, 0, 0) to (arrowWidth, 0, +arrowLength)
            val rightLine = CylinderGeometry(
                radiusTop = lineThickness,
                radiusBottom = lineThickness,
                height = lineLength,
                radialSegments = 4,
            ).apply {
                // Cylinder is along Y axis by default, rotate to point correctly
                rotateZ(-angle)  // Rotate around Z to angle the line
                rotateX(-kotlin.math.PI / 2)  // Rotate to XZ plane
                // Move to midpoint of the line
                translate(arrowWidth / 2, 0.0, arrowLength / 2)
            }

            // Merge the two cylinders into one geometry
            return BufferGeometryUtils.mergeBufferGeometries(
                arrayOf(leftLine, rightLine),
                useGroups = false,
            )!!.apply {
                computeBoundingBox()
                computeBoundingSphere()
            }
        }
    }
}
