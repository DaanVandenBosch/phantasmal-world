package world.phantasmal.web.questEditor

import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.web.core.PwTool
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.rendering.DisposableThreeRenderer
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.core.undo.UndoManager
import world.phantasmal.web.questEditor.controllers.*
import world.phantasmal.web.questEditor.loading.AreaAssetLoader
import world.phantasmal.web.questEditor.loading.EntityAssetLoader
import world.phantasmal.web.questEditor.loading.QuestLoader
import world.phantasmal.web.questEditor.persistence.QuestEditorUiPersister
import world.phantasmal.web.questEditor.rendering.EntityImageRenderer
import world.phantasmal.web.questEditor.rendering.QuestRenderer
import world.phantasmal.web.questEditor.stores.AreaStore
import world.phantasmal.web.questEditor.stores.AsmStore
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

    override fun initialize(): Widget {
        // Asset Loaders
        val questLoader = addDisposable(QuestLoader(assetLoader))
        val areaAssetLoader = addDisposable(AreaAssetLoader(assetLoader))
        val entityAssetLoader = addDisposable(EntityAssetLoader(assetLoader))

        // Persistence
        val questEditorUiPersister = QuestEditorUiPersister()

        // Undo
        val undoManager = UndoManager()

        // Stores
        val areaStore = addDisposable(AreaStore(areaAssetLoader))
        val questEditorStore = addDisposable(QuestEditorStore(
            uiStore,
            areaStore,
            undoManager,
        ))
        val asmStore = addDisposable(AsmStore(questEditorStore, undoManager))

        // Controllers
        val questEditorController = addDisposable(QuestEditorController(questEditorUiPersister))
        val toolbarController = addDisposable(QuestEditorToolbarController(
            uiStore,
            questLoader,
            areaStore,
            questEditorStore,
        ))
        val questInfoController = addDisposable(QuestInfoController(questEditorStore))
        val npcCountsController = addDisposable(NpcCountsController(questEditorStore))
        val entityInfoController = addDisposable(EntityInfoController(questEditorStore))
        val asmController = addDisposable(AsmController(asmStore))
        val npcListController = addDisposable(EntityListController(questEditorStore, npcs = true))
        val objectListController =
            addDisposable(EntityListController(questEditorStore, npcs = false))

        // Rendering
        val renderer = addDisposable(QuestRenderer(
            areaAssetLoader,
            entityAssetLoader,
            questEditorStore,
            createThreeRenderer,
        ))
        val entityImageRenderer = EntityImageRenderer(entityAssetLoader, createThreeRenderer)

        // Main Widget
        return QuestEditorWidget(
            questEditorController,
            { QuestEditorToolbarWidget(toolbarController) },
            { QuestInfoWidget(questInfoController) },
            { NpcCountsWidget(npcCountsController) },
            { EntityInfoWidget(entityInfoController) },
            { QuestEditorRendererWidget(renderer) },
            { AsmWidget(asmController) },
            { EntityListWidget(npcListController, entityImageRenderer) },
            { EntityListWidget(objectListController, entityImageRenderer) },
        )
    }
}
