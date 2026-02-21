package world.phantasmal.web.questEditor.rendering

import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.web.core.rendering.DisposableThreeRenderer
import world.phantasmal.web.core.rendering.Renderer
import world.phantasmal.web.externals.three.PerspectiveCamera
import world.phantasmal.web.questEditor.loading.AreaAssetLoader
import world.phantasmal.web.questEditor.loading.EntityAssetLoader
import world.phantasmal.web.questEditor.rendering.input.QuestInputManager
import world.phantasmal.web.questEditor.stores.AreaStore
import world.phantasmal.web.questEditor.stores.QuestEditorStore

class QuestRenderer(
    areaAssetLoader: AreaAssetLoader,
    entityAssetLoader: EntityAssetLoader,
    questEditorStore: QuestEditorStore,
    areaStore: AreaStore,
    createThreeRenderer: (HTMLCanvasElement) -> DisposableThreeRenderer,
) : Renderer() {
    override val context = addDisposable(QuestRenderContext(
        createCanvas(),
        PerspectiveCamera(
            fov = 45.0,
            aspect = 1.0,
            near = 10.0,
            far = 5_000.0,
        ),
    ))

    override val threeRenderer = addDisposable(createThreeRenderer(context.canvas)).renderer

    override val inputManager = addDisposable(QuestInputManager(questEditorStore, context))

    private val meshManager = addDisposable(
        QuestEditorMeshManager(
            areaAssetLoader,
            entityAssetLoader,
            questEditorStore,
            areaStore,
            context,
        ),
    )

    init {

        var prevQuest = questEditorStore.currentQuest.value
        var prevAreaVariant = questEditorStore.currentAreaVariant.value

        observeNow(questEditorStore.currentQuest, questEditorStore.currentAreaVariant) { q, av ->
            if (q !== prevQuest || av !== prevAreaVariant) {
                inputManager.resetCamera()

                prevQuest = q
                prevAreaVariant = av
            }
        }
    }

    override fun render() {
        // Call parent render method (handles inputManager.beforeRender() and actual rendering)
        super.render()
    }
}
