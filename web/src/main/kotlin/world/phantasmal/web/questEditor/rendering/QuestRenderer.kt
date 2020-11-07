package world.phantasmal.web.questEditor.rendering

import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.web.core.rendering.Renderer
import world.phantasmal.web.externals.babylon.*
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.rendering.conversion.EntityMetadata
import kotlin.math.PI

class QuestRenderer(canvas: HTMLCanvasElement, engine: Engine) : Renderer(canvas, engine) {
    private var entityMeshes = TransformNode("Entities", scene)
    private val entityToMesh = mutableMapOf<QuestEntityModel<*, *>, AbstractMesh>()

    override val camera = ArcRotateCamera("Camera", PI / 2, PI / 6, 500.0, Vector3.Zero(), scene)

    init {
        with(camera) {
            attachControl(
                canvas,
                noPreventDefault = false,
                useCtrlForPanning = false,
                panningMouseButton = 0
            )
            inertia = 0.0
            angularSensibilityX = 200.0
            angularSensibilityY = 200.0
            panningInertia = 0.0
            panningSensibility = 3.0
            panningAxis = Vector3(1.0, 0.0, -1.0)
            pinchDeltaPercentage = 0.1
            wheelDeltaPercentage = 0.1
        }
    }

    override fun internalDispose() {
        entityMeshes.dispose()
        entityToMesh.clear()
        super.internalDispose()
    }

    fun resetEntityMeshes() {
        entityMeshes.dispose(false)
        entityToMesh.clear()

        entityMeshes = TransformNode("Entities", scene)
    }

    fun addEntityMesh(mesh: AbstractMesh) {
        val entity = (mesh.metadata as EntityMetadata).entity
        mesh.parent = entityMeshes

        entityToMesh[entity]?.let { prevMesh ->
            prevMesh.parent = null
            prevMesh.dispose()
        }

        entityToMesh[entity] = mesh

        // TODO: Mark selected entity.
//        if (entity === this.selected_entity) {
//            this.mark_selected(model)
//        }
    }

    fun removeEntityMesh(entity: QuestEntityModel<*, *>) {
        entityToMesh.remove(entity)?.let { mesh ->
            mesh.parent = null
            mesh.dispose()
        }
    }
}
