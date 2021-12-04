package world.phantasmal.web.questEditor.rendering

import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.web.core.rendering.DisposableThreeRenderer
import world.phantasmal.web.core.rendering.Renderer
import world.phantasmal.web.externals.three.PerspectiveCamera
import world.phantasmal.web.questEditor.loading.AreaAssetLoader
import world.phantasmal.web.questEditor.loading.EntityAssetLoader
import world.phantasmal.web.questEditor.rendering.input.QuestInputManager
import world.phantasmal.web.questEditor.stores.QuestEditorStore

class QuestRenderer(
    areaAssetLoader: AreaAssetLoader,
    entityAssetLoader: EntityAssetLoader,
    questEditorStore: QuestEditorStore,
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

    init {
        addDisposables(
            QuestEditorMeshManager(
                areaAssetLoader,
                entityAssetLoader,
                questEditorStore,
                context,
            ),
        )

        observeNow(questEditorStore.currentQuest) { inputManager.resetCamera() }
        observeNow(questEditorStore.currentAreaVariant) { inputManager.resetCamera() }
    }
}
