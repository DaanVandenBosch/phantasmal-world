package world.phantasmal.web.application

import kotlinx.browser.document
import kotlinx.datetime.Clock
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
import world.phantasmal.web.core.PwTool
import world.phantasmal.web.core.loading.AssetLoader
import world.phantasmal.web.core.persistence.KeyValueStore
import world.phantasmal.web.core.rendering.DisposableThreeRenderer
import world.phantasmal.web.core.stores.ApplicationUrl
import world.phantasmal.web.core.stores.UiStore
import world.phantasmal.web.huntOptimizer.HuntOptimizer
import world.phantasmal.web.questEditor.QuestEditor
import world.phantasmal.web.viewer.Viewer
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.dom.disposableListener

class Application(
    rootElement: HTMLElement,
    keyValueStore: KeyValueStore,
    assetLoader: AssetLoader,
    applicationUrl: ApplicationUrl,
    createThreeRenderer: (HTMLCanvasElement) -> DisposableThreeRenderer,
    clock: Clock,
) : DisposableContainer() {
    init {
        addDisposables(
            // Disable native undo/redo.
            document.disposableListener("beforeinput", ::beforeInput),
            // Disable native undo/redo in FireFox.
            document.disposableListener("keydown", ::keydown),

            // Disable native drag-and-drop to avoid users dragging in unsupported file formats and
            // leaving the application unexpectedly.
            document.disposableListener("dragenter", ::dragenter),
            document.disposableListener("dragover", ::dragover),
            document.disposableListener("drop", ::drop),
        )

        // Initialize core stores shared by several submodules.
        val uiStore = addDisposable(UiStore(applicationUrl))

        // The various tools Phantasmal World consists of.
        val tools: List<PwTool> = listOf(
            addDisposable(Viewer(assetLoader, uiStore, createThreeRenderer)),
            addDisposable(QuestEditor(keyValueStore, assetLoader, uiStore, createThreeRenderer)),
            addDisposable(HuntOptimizer(keyValueStore, assetLoader, uiStore)),
        )

        // Controllers.
        val navigationController = addDisposable(NavigationController(uiStore, clock))
        val mainContentController = addDisposable(MainContentController(uiStore))

        // Initialize application view.
        val applicationWidget = addDisposable(
            ApplicationWidget(
                { NavigationWidget(navigationController) },
                {
                    MainContentWidget(
                        mainContentController,
                        tools.associate { it.toolType to it::initialize },
                    )
                }
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
        if ((e.ctrlKey || e.metaKey) && !e.altKey && e.key.uppercase() == "Z") {
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
