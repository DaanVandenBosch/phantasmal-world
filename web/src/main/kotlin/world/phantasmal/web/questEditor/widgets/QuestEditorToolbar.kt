package world.phantasmal.web.questEditor.widgets

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.w3c.dom.Node
import world.phantasmal.lib.fileFormats.quest.Episode
import world.phantasmal.web.questEditor.controllers.QuestEditorToolbarController
import world.phantasmal.webui.dom.Icon
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.Button
import world.phantasmal.webui.widgets.FileButton
import world.phantasmal.webui.widgets.Toolbar
import world.phantasmal.webui.widgets.Widget

class QuestEditorToolbar(
    scope: CoroutineScope,
    private val ctrl: QuestEditorToolbarController,
) : Widget(scope) {
    override fun Node.createElement() =
        div {
            className = "pw-quest-editor-toolbar"

            addChild(Toolbar(
                scope,
                children = listOf(
                    Button(
                        scope,
                        text = "New quest",
                        iconLeft = Icon.NewFile,
                        onClick = { scope.launch { ctrl.createNewQuest(Episode.I) } }
                    ),
                    FileButton(
                        scope,
                        text = "Open file...",
                        iconLeft = Icon.File,
                        accept = ".bin, .dat, .qst",
                        multiple = true,
                        filesSelected = { files -> scope.launch { ctrl.openFiles(files) } }
                    )
                )
            ))
        }
}
