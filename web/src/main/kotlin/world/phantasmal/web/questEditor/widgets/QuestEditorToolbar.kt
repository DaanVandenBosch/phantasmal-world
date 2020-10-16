package world.phantasmal.web.questEditor.widgets

import org.w3c.dom.Node
import world.phantasmal.core.disposable.Scope
import world.phantasmal.web.questEditor.controllers.QuestEditorToolbarController
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.FileButton
import world.phantasmal.webui.widgets.Toolbar
import world.phantasmal.webui.widgets.Widget

class QuestEditorToolbar(
    scope: Scope,
    private val ctrl: QuestEditorToolbarController,
) : Widget(scope) {
    override fun Node.createElement() = div(className = "pw-quest-editor-toolbar") {
        addChild(Toolbar(
            scope,
            children = listOf(
                FileButton(
                    scope,
                    text = "Open file...",
                    accept = ".bin, .dat, .qst",
                    multiple = true,
                    filesSelected = ctrl::filesOpened
                )
            )
        ))
    }
}
