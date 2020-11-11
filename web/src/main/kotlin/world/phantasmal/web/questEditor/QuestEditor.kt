package world.phantasmal.web.questEditor

import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.web.core.PwTool
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.externals.babylon.Engine
import world.phantasmal.web.questEditor.controllers.NpcCountsController
import world.phantasmal.web.questEditor.controllers.QuestEditorToolbarController
import world.phantasmal.web.questEditor.controllers.QuestInfoController
import world.phantasmal.web.questEditor.loading.AreaAssetLoader
import world.phantasmal.web.questEditor.loading.EntityAssetLoader
import world.phantasmal.web.questEditor.loading.QuestLoader
import world.phantasmal.web.questEditor.rendering.QuestEditorMeshManager
import world.phantasmal.web.questEditor.rendering.QuestRenderer
import world.phantasmal.web.questEditor.rendering.UserInputManager
import world.phantasmal.web.questEditor.stores.AreaStore
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.web.questEditor.widgets.*
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.widgets.Widget

class QuestEditor(
    private val assetLoader: AssetLoader,
    private val uiStore: UiStore,
    private val createEngine: (HTMLCanvasElement) -> Engine,
) : DisposableContainer(), PwTool {
    override val toolType = PwToolType.QuestEditor

    override fun initialize(scope: CoroutineScope): Widget {
        // Renderer
        val canvas = document.createElement("CANVAS") as HTMLCanvasElement
        val renderer = addDisposable(QuestRenderer(canvas, createEngine(canvas)))

        // Asset Loaders
        val questLoader = addDisposable(QuestLoader(scope, assetLoader))
        val areaAssetLoader = addDisposable(AreaAssetLoader(scope, assetLoader, renderer.scene))
        val entityAssetLoader = addDisposable(EntityAssetLoader(scope, assetLoader, renderer.scene))

        // Stores
        val areaStore = addDisposable(AreaStore(scope, areaAssetLoader))
        val questEditorStore = addDisposable(QuestEditorStore(scope, uiStore, areaStore))

        // Controllers
        val toolbarController = addDisposable(QuestEditorToolbarController(
            questLoader,
            areaStore,
            questEditorStore,
        ))
        val questInfoController = addDisposable(QuestInfoController(questEditorStore))
        val npcCountsController = addDisposable(NpcCountsController(questEditorStore))

        // Rendering
        addDisposables(
            QuestEditorMeshManager(
                scope,
                questEditorStore,
                renderer,
                areaAssetLoader,
                entityAssetLoader
            ),
            UserInputManager(questEditorStore, renderer)
        )

        // Main Widget
        return QuestEditorWidget(
            scope,
            { s -> QuestEditorToolbarWidget(s, toolbarController) },
            { s -> QuestInfoWidget(s, questInfoController) },
            { s -> NpcCountsWidget(s, npcCountsController) },
            { s -> QuestEditorRendererWidget(s, canvas, renderer) }
        )
    }
}
