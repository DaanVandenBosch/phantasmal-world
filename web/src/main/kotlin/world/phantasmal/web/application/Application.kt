package world.phantasmal.web.application

import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.DragEvent
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import world.phantasmal.core.disposable.Scope
import world.phantasmal.web.application.controllers.MainContentController
import world.phantasmal.web.application.controllers.NavigationController
import world.phantasmal.web.application.widgets.ApplicationWidget
import world.phantasmal.web.application.widgets.MainContentWidget
import world.phantasmal.web.application.widgets.NavigationWidget
import world.phantasmal.web.core.AssetLoader
import world.phantasmal.web.core.stores.ApplicationUrl
import world.phantasmal.web.core.stores.PwTool
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.externals.Engine
import world.phantasmal.web.huntOptimizer.HuntOptimizer
import world.phantasmal.web.questEditor.QuestEditor
import world.phantasmal.webui.dom.disposableListener

class Application(
    scope: Scope,
    rootElement: HTMLElement,
    assetLoader: AssetLoader,
    applicationUrl: ApplicationUrl,
    createEngine: (HTMLCanvasElement) -> Engine,
) {
    init {
        // Disable native undo/redo.
        disposableListener(scope, document, "beforeinput", ::beforeInput)
        // Work-around for FireFox:
        disposableListener(scope, document, "keydown", ::keydown)

        // Disable native drag-and-drop to avoid users dragging in unsupported file formats and
        // leaving the application unexpectedly.
        disposableListener(scope, document, "dragenter", ::dragenter)
        disposableListener(scope, document, "dragover", ::dragover)
        disposableListener(scope, document, "drop", ::drop)

        // Initialize core stores shared by several submodules.
        val uiStore = UiStore(scope, applicationUrl)

        // Controllers.
        val navigationController = NavigationController(scope, uiStore)
        val mainContentController = MainContentController(scope, uiStore)

        // Initialize application view.
        val applicationWidget = ApplicationWidget(
            scope,
            NavigationWidget(scope, navigationController),
            MainContentWidget(scope, mainContentController, mapOf(
                PwTool.QuestEditor to { s ->
                    QuestEditor(s, uiStore, createEngine).widget
                },
                PwTool.HuntOptimizer to { s ->
                    HuntOptimizer(s, assetLoader, uiStore).widget
                },
            ))
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
