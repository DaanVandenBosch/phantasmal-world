package world.phantasmal.web.application

import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.DragEvent
import org.w3c.dom.Node
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import world.phantasmal.core.disposable.DisposableContainer
import world.phantasmal.web.application.controllers.MainContentController
import world.phantasmal.web.application.controllers.NavigationController
import world.phantasmal.web.application.widgets.ApplicationWidget
import world.phantasmal.web.application.widgets.MainContentWidget
import world.phantasmal.web.application.widgets.NavigationWidget
import world.phantasmal.web.core.AssetLoader
import world.phantasmal.web.core.stores.ApplicationUrl
import world.phantasmal.web.core.stores.PwTool
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.huntOptimizer.HuntOptimizer
import world.phantasmal.webui.dom.disposableListener

class Application(
    scope: CoroutineScope,
    rootNode: Node,
    assetLoader: AssetLoader,
    applicationUrl: ApplicationUrl,
) : DisposableContainer() {
    init {
        // Disable native undo/redo.
        addDisposable(disposableListener(document, "beforeinput", ::beforeInput))
        // Work-around for FireFox:
        addDisposable(disposableListener(document, "keydown", ::keydown))

        // Disable native drag-and-drop to avoid users dragging in unsupported file formats and
        // leaving the application unexpectedly.
        addDisposables(
            disposableListener(document, "dragenter", ::dragenter),
            disposableListener(document, "dragover", ::dragover),
            disposableListener(document, "drop", ::drop),
        )

        // Initialize core stores shared by several submodules.
        val uiStore = addDisposable(UiStore(scope, applicationUrl))

        // Controllers.
        val navigationController = addDisposable(NavigationController(uiStore))
        val mainContentController = addDisposable(MainContentController(uiStore))

        // Initialize application view.
        val applicationWidget = addDisposable(
            ApplicationWidget(
                addDisposable(NavigationWidget(navigationController)),
                addDisposable(MainContentWidget(mainContentController, mapOf(
                    PwTool.HuntOptimizer to {
                        addDisposable(HuntOptimizer(scope, assetLoader, uiStore)).widget
                    }
                ))),
            ),
        )

        rootNode.appendChild(applicationWidget.element)
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
