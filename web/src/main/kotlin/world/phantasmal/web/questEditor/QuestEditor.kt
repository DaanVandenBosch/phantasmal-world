package world.phantasmal.web.questEditor

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.web.core.PwTool
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.rendering.DisposableThreeRenderer
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.questEditor.controllers.*
import world.phantasmal.web.questEditor.loading.AreaAssetLoader
import world.phantasmal.web.questEditor.loading.EntityAssetLoader
import world.phantasmal.web.questEditor.loading.QuestLoader
import world.phantasmal.web.questEditor.rendering.QuestRenderer
import world.phantasmal.web.questEditor.stores.AreaStore
import world.phantasmal.web.questEditor.stores.AssemblyEditorStore
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.web.questEditor.widgets.*
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.widgets.Widget

class QuestEditor(
    private val assetLoader: AssetLoader,
    private val uiStore: UiStore,
    private val createThreeRenderer: (HTMLCanvasElement) -> DisposableThreeRenderer,
) : DisposableContainer(), PwTool {
    override val toolType = PwToolType.QuestEditor

    override fun initialize(scope: CoroutineScope): Widget {
        // Asset Loaders
        val questLoader = addDisposable(QuestLoader(scope, assetLoader))
        val areaAssetLoader = addDisposable(AreaAssetLoader(scope, assetLoader))
        val entityAssetLoader = addDisposable(EntityAssetLoader(scope, assetLoader))

        // Stores
        val areaStore = addDisposable(AreaStore(scope, areaAssetLoader))
        val questEditorStore = addDisposable(QuestEditorStore(scope, uiStore, areaStore))
        val assemblyEditorStore = addDisposable(AssemblyEditorStore(scope, questEditorStore))

        // Controllers
        val toolbarController = addDisposable(QuestEditorToolbarController(
            questLoader,
            areaStore,
            questEditorStore,
        ))
        val questInfoController = addDisposable(QuestInfoController(questEditorStore))
        val npcCountsController = addDisposable(NpcCountsController(questEditorStore))
        val entityInfoController = addDisposable(EntityInfoController(questEditorStore))
        val assemblyEditorController = addDisposable(AssemblyEditorController(assemblyEditorStore))

        // Rendering
        val renderer = addDisposable(QuestRenderer(
            scope,
            areaAssetLoader,
            entityAssetLoader,
            questEditorStore,
            createThreeRenderer,
        ))

        // Main Widget
        return QuestEditorWidget(
            scope,
            { s -> QuestEditorToolbarWidget(s, toolbarController) },
            { s -> QuestInfoWidget(s, questInfoController) },
            { s -> NpcCountsWidget(s, npcCountsController) },
            { s -> EntityInfoWidget(s, entityInfoController) },
            { s -> QuestEditorRendererWidget(s, renderer) },
            { s -> AssemblyEditorWidget(s, assemblyEditorController) },
        )
    }
}
