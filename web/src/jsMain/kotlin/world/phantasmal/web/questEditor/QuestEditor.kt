package world.phantasmal.web.questEditor

import kotlinx.browser.window
import org.w3c.dom.BeforeUnloadEvent
import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.web.core.PwTool
import world.phantasmal.web.core.PwToolType
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.persistence.KeyValueStore
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
import world.phantasmal.webui.dom.disposableListener
import world.phantasmal.webui.widgets.Widget

class QuestEditor(
    private val keyValueStore: KeyValueStore,
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
        val questEditorUiPersister = QuestEditorUiPersister(keyValueStore)

        // Undo
        val undoManager = UndoManager()

        // Stores
        val areaStore = addDisposable(AreaStore(areaAssetLoader))
        val questEditorStore = addDisposable(
            QuestEditorStore(
                questLoader,
                uiStore,
                areaStore,
                undoManager,
                initializeNewQuest = true,
            )
        )
        val asmStore = addDisposable(AsmStore(questEditorStore, undoManager))

        // Controllers
        val questEditorController = addDisposable(QuestEditorController(questEditorUiPersister))
        val toolbarController = addDisposable(
            QuestEditorToolbarController(
                uiStore,
                areaStore,
                questEditorStore,
            )
        )
        val questInfoController = addDisposable(QuestInfoController(questEditorStore))
        val npcCountsController = addDisposable(NpcCountsController(questEditorStore))
        val entityInfoController = addDisposable(EntityInfoController(areaStore, questEditorStore))
        val asmController = addDisposable(AsmEditorController(asmStore))
        val npcListController = addDisposable(EntityListController(questEditorStore, npcs = true))
        val objectListController =
            addDisposable(EntityListController(questEditorStore, npcs = false))
        val eventsController = addDisposable(EventsController(questEditorStore))

        // Rendering
        val renderer = addDisposable(
            QuestRenderer(
                areaAssetLoader,
                entityAssetLoader,
                questEditorStore,
                areaStore,
                createThreeRenderer,
            )
        )
        val entityImageRenderer =
            addDisposable(EntityImageRenderer(entityAssetLoader, createThreeRenderer))

        // When the user tries to leave and there are unsaved changes, ask whether the user really
        // wants to leave.
        addDisposable(
            window.disposableListener("beforeunload", { e: BeforeUnloadEvent ->
                if (!undoManager.allAtSavePoint.value) {
                    e.preventDefault()
                    e.returnValue = "false"
                }
            })
        )

        // Main Widget
        return QuestEditorWidget(
            questEditorController,
            { QuestEditorToolbarWidget(toolbarController) },
            { QuestInfoWidget(questInfoController) },
            { NpcCountsWidget(npcCountsController) },
            { EntityInfoWidget(entityInfoController) },
            { QuestEditorRendererWidget(renderer, questEditorStore.mouseWorldPosition) },
            { AsmWidget(asmController) },
            { EntityListWidget(npcListController, entityImageRenderer, questEditorStore, isNpcList = true) },
            { EntityListWidget(objectListController, entityImageRenderer, questEditorStore, isNpcList = false) },
            { EventsWidget(eventsController) },
        )
    }
}
