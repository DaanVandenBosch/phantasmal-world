package world.phantasmal.web.questEditor.rendering

import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.web.core.rendering.Renderer
import world.phantasmal.web.externals.babylon.*
import world.phantasmal.web.questEditor.models.QuestEntityModel
import world.phantasmal.web.questEditor.rendering.conversion.EntityMetadata
import kotlin.math.PI

class QuestRenderer(
    canvas: HTMLCanvasElement,
    engine: Engine,
    createMeshManager: (QuestRenderer, Scene) -> QuestMeshManager,
) : Renderer(canvas, engine) {
    private val meshManager = createMeshManager(this, scene)
    private var entityMeshes = TransformNode("Entities", scene)
    private val entityToMesh = mutableMapOf<QuestEntityModel<*, *>, AbstractMesh>()
    private val camera = ArcRotateCamera("Camera", 0.0, PI / 6, 500.0, Vector3.Zero(), scene)
    private val light = HemisphericLight("Light", Vector3(1.0, 1.0, 0.0), scene)

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
            panningAxis = Vector3(1.0, 0.0, 1.0)
            pinchDeltaPercentage = 0.1
            wheelDeltaPercentage = 0.1
        }
    }

    override fun internalDispose() {
        meshManager.dispose()
        entityMeshes.dispose()
        entityToMesh.clear()
        camera.dispose()
        light.dispose()
        super.internalDispose()
    }

    fun resetEntityMeshes() {
        entityMeshes.dispose(false)
        entityToMesh.clear()

        entityMeshes = TransformNode("Entities", scene)
        scheduleRender()
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

        this.scheduleRender()
    }

    fun removeEntityMesh(entity: QuestEntityModel<*, *>) {
        entityToMesh.remove(entity)?.let { mesh ->
            mesh.parent = null
            mesh.dispose()
            this.scheduleRender()
        }
    }
}
