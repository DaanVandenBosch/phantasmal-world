package world.phantasmal.web.application

import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.DragEvent
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import world.phantasmal.web.application.controllers.MainContentController
import world.phantasmal.web.application.controllers.NavigationController
import world.phantasmal.web.application.widgets.ApplicationWidget
import world.phantasmal.web.application.widgets.MainContentWidget
import world.phantasmal.web.application.widgets.NavigationWidget
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.stores.ApplicationUrl
import world.phantasmal.web.core.stores.PwTool
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.externals.babylon.Engine
import world.phantasmal.web.huntOptimizer.HuntOptimizer
import world.phantasmal.web.questEditor.QuestEditor
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.dom.disposableListener

class Application(
    scope: CoroutineScope,
    rootElement: HTMLElement,
    assetLoader: AssetLoader,
    applicationUrl: ApplicationUrl,
    createEngine: (HTMLCanvasElement) -> Engine,
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

        // Controllers.
        val navigationController = addDisposable(NavigationController(scope, uiStore))
        val mainContentController = addDisposable(MainContentController(scope, uiStore))

        // Initialize application view.
        val applicationWidget = addDisposable(
            ApplicationWidget(
                scope,
                NavigationWidget(scope, navigationController),
                MainContentWidget(scope, mainContentController, mapOf(
                    PwTool.QuestEditor to { widgetScope ->
                        addDisposable(QuestEditor(
                            widgetScope,
                            assetLoader,
                            createEngine
                        )).createWidget()
                    },
                    PwTool.HuntOptimizer to { widgetScope ->
                        addDisposable(HuntOptimizer(
                            widgetScope,
                            assetLoader,
                            uiStore
                        )).createWidget()
                    },
                ))
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
