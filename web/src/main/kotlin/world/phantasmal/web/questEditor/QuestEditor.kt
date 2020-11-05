package world.phantasmal.web.questEditor

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.HTMLCanvasElement
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.externals.babylon.Engine
import world.phantasmal.web.questEditor.controllers.NpcCountsController
import world.phantasmal.web.questEditor.controllers.QuestEditorToolbarController
import world.phantasmal.web.questEditor.controllers.QuestInfoController
import world.phantasmal.web.questEditor.loading.EntityAssetLoader
import world.phantasmal.web.questEditor.loading.QuestLoader
import world.phantasmal.web.questEditor.rendering.QuestEditorMeshManager
import world.phantasmal.web.questEditor.rendering.QuestRenderer
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.web.questEditor.widgets.*
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.widgets.Widget

class QuestEditor(
    private val scope: CoroutineScope,
    private val assetLoader: AssetLoader,
    private val createEngine: (HTMLCanvasElement) -> Engine,
) : DisposableContainer() {
    // Asset Loaders
    private val questLoader = addDisposable(QuestLoader(scope, assetLoader))

    // Stores
    private val questEditorStore = addDisposable(QuestEditorStore(scope))

    // Controllers
    private val toolbarController =
        addDisposable(QuestEditorToolbarController(scope, questLoader, questEditorStore))
    private val questInfoController = addDisposable(QuestInfoController(scope, questEditorStore))
    private val npcCountsController = addDisposable(NpcCountsController(scope, questEditorStore))

    fun createWidget(): Widget =
        QuestEditorWidget(
            scope,
            QuestEditorToolbar(scope, toolbarController),
            { scope -> QuestInfoWidget(scope, questInfoController) },
            { scope -> NpcCountsWidget(scope, npcCountsController) },
            { scope -> QuestEditorRendererWidget(scope, ::createQuestEditorRenderer) }
        )

    private fun createQuestEditorRenderer(canvas: HTMLCanvasElement): QuestRenderer =
        QuestRenderer(canvas, createEngine(canvas)) { renderer, scene ->
            QuestEditorMeshManager(
                scope,
                questEditorStore.currentQuest,
                questEditorStore.currentArea,
                questEditorStore.selectedWave,
                renderer,
                EntityAssetLoader(scope, assetLoader, scene)
            )
        }
}
