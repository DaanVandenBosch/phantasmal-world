package world.phantasmal.web.application

import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.DragEvent
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import world.phantasmal.web.application.controllers.MainContentController
import world.phantasmal.web.application.controllers.NavigationController
import world.phantasmal.web.application.widgets.ApplicationWidget
import world.phantasmal.web.application.widgets.MainContentWidget
import world.phantasmal.web.application.widgets.NavigationWidget
import world.phantasmal.web.core.PwTool
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.rendering.DisposableThreeRenderer
import world.phantasmal.web.core.stores.ApplicationUrl
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.huntOptimizer.HuntOptimizer
import world.phantasmal.web.questEditor.QuestEditor
import world.phantasmal.web.viewer.Viewer
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.dom.disposableListener

class Application(
    scope: CoroutineScope,
    rootElement: HTMLElement,
    assetLoader: AssetLoader,
    applicationUrl: ApplicationUrl,
    createThreeRenderer: () -> DisposableThreeRenderer,
) : DisposableContainer() {
    init {
        addDisposables(
            // Disable native undo/redo.
            disposableListener(document, "beforeinput", ::beforeInput),
            // Work-around for FireFox:
            disposableListener(document, "keydown", ::keydown),

            // Disable native drag-and-drop to avoid users dragging in unsupported file formats and
            // leaving the application unexpectedly.
            disposableListener(document, "dragenter", ::dragenter),
            disposableListener(document, "dragover", ::dragover),
            disposableListener(document, "drop", ::drop),
        )

        // Initialize core stores shared by several submodules.
        val uiStore = addDisposable(UiStore(scope, applicationUrl))

        // The various tools Phantasmal World consists of.
        val tools: List<PwTool> = listOf(
            Viewer(createThreeRenderer),
            QuestEditor(assetLoader, uiStore, createThreeRenderer),
            HuntOptimizer(assetLoader, uiStore),
        )

        // Controllers.
        val navigationController = addDisposable(NavigationController(uiStore))
        val mainContentController = addDisposable(MainContentController(uiStore))

        // Initialize application view.
        val applicationWidget = addDisposable(
            ApplicationWidget(
                scope,
                NavigationWidget(scope, navigationController),
                MainContentWidget(
                    scope,
                    mainContentController,
                    tools.map { it.toolType to it::initialize }.toMap()
                )
            )
        )

        rootElement.appendChild(applicationWidget.element)
    }

    private fun beforeInput(e: Event) {
        val ie = e.asDynamic()

        if (ie.inputType == "historyUndo" || ie.inputType == "historyRedo") {
            e.preventDefault()
        }
    }

    private fun keydown(e: KeyboardEvent) {
        if (e.ctrlKey && !e.altKey && e.key.toUpperCase() == "Z") {
            e.preventDefault()
        }
    }

    private fun dragenter(e: DragEvent) {
        e.preventDefault()

        e.dataTransfer?.let {
            it.dropEffect = "none"
        }
    }

    private fun dragover(e: DragEvent) {
        dragenter(e)
    }

    private fun drop(e: DragEvent) {
        dragenter(e)
    }
}
