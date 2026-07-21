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
import world.phantasmal.web.viewer.loading.CharacterClassAssetLoader
import world.phantasmal.web.questEditor.loading.AreaAssetLoader
import world.phantasmal.web.questEditor.loading.BattleParamRepository
import world.phantasmal.web.questEditor.loading.EntityAssetLoader
import world.phantasmal.web.questEditor.loading.QuestLoader
import world.phantasmal.web.questEditor.loading.ParticleAssetLoader
import world.phantasmal.web.questEditor.loading.SymbolChatColliRepository
import world.phantasmal.web.questEditor.persistence.QuestEditorUiPersister
import world.phantasmal.web.questEditor.rendering.EntityImageRenderer
import world.phantasmal.web.questEditor.rendering.QuestRenderer
import world.phantasmal.web.questEditor.stores.*
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
        val battleParamRepository = addDisposable(BattleParamRepository(assetLoader))
        val symbolChatColliRepository = addDisposable(SymbolChatColliRepository(assetLoader))

        // Pre-warm the SymbolChatRenderer atlas so the first hover/selection
        // that paints SC stages doesn't flash a blank canvas while the four
        // sega_*.png atlases (~100 KB total) load. Nothing to do on success —
        // ensureLoaded just kicks off loading and caches the result.
        SymbolChatRenderer.ensureLoaded {}

        // Persistence
        val questEditorUiPersister = QuestEditorUiPersister(keyValueStore)

        // Undo
        val undoManager = UndoManager()

        // Stores
        val areaStore = addDisposable(AreaStore(areaAssetLoader))
        val questEditorUiStore = addDisposable(QuestEditorUiStore(uiStore))
        val playbackVisualizationStore = addDisposable(PlaybackVisualizationStore())
        val viewportStore = addDisposable(ViewportStore())
        val questEditorStore = addDisposable(
            QuestEditorStore(
                questLoader,
                uiStore,
                areaStore,
                undoManager,
                viewportStore,
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
                questEditorUiStore,
            )
        )
        val questInfoController = addDisposable(QuestInfoController(questEditorStore))
        val npcCountsController = addDisposable(NpcCountsController(questEditorStore))
        val asmController = addDisposable(AsmEditorController(asmStore))
        val dataEditorController = addDisposable(DataEditorController(questEditorStore, asmStore, battleParamRepository))
        val entityInfoController = addDisposable(EntityInfoController(
            areaStore,
            questEditorStore,
            asmStore,
            onActivateAsmEditor = {
                questEditorController.requestActivateWidget(
                    QuestEditorController.ASM_WIDGET_ID
                )
            },
        ))
        val characterClassAssetLoader = addDisposable(CharacterClassAssetLoader(assetLoader))
        val particleAssetLoader = addDisposable(ParticleAssetLoader(assetLoader))
        val npcListController = addDisposable(EntityListController(questEditorStore, questEditorUiStore, npcs = true))
        val objectListController =
            addDisposable(EntityListController(questEditorStore, questEditorUiStore, npcs = false))
        val eventsController = addDisposable(EventsController(questEditorStore, playbackVisualizationStore))
        val areaNpcListController = addDisposable(AreaNpcListController(questEditorStore, viewportStore))
        val areaObjectListController = addDisposable(AreaObjectListController(questEditorStore, viewportStore))
        val monsterRandomnessController = addDisposable(MonsterRandomnessController(questEditorStore))
        val compatibilityController = addDisposable(CompatibilityController(questEditorStore, asmStore))

        // Rendering
        val renderer = addDisposable(
            QuestRenderer(
                areaAssetLoader,
                entityAssetLoader,
                particleAssetLoader,
                questEditorStore,
                questEditorUiStore,
                playbackVisualizationStore,
                viewportStore,
                areaStore,
                symbolChatColliRepository,
                dataEditorController.symbolChatTriggers,
                dataEditorController::readSegmentData,
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
            { QuestEditorToolbarWidget(toolbarController, compatibilityController) },
            { QuestInfoWidget(questInfoController) },
            { NpcCountsWidget(npcCountsController) },
            { EntityInfoWidget(entityInfoController) },
            {
                QuestEditorRendererWidget(
                    renderer,
                    viewportStore.mouseWorldPosition,
                    playbackVisualizationStore.playbackActionText,
                    questEditorStore,
                    questEditorUiStore,
                    viewportStore,
                    monsterRandomnessController,
                    areaNpcListController,
                    areaObjectListController,
                    symbolChatColliRepository,
                )
            },
            { AsmWidget(asmController, dataEditorController, characterClassAssetLoader, symbolChatColliRepository, createThreeRenderer) },
            { EntityListWidget(npcListController, entityImageRenderer, questEditorUiStore, isNpcList = true) },
            { EntityListWidget(objectListController, entityImageRenderer, questEditorUiStore, isNpcList = false) },
            { EventsWidget(eventsController) },
        )
    }
}
