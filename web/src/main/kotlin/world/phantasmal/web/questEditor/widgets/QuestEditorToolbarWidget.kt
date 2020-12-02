package world.phantasmal.web.questEditor.widgets

import kotlinx.coroutines.launch
import org.w3c.dom.Node
import world.phantasmal.lib.fileFormats.quest.Episode
import world.phantasmal.observable.value.value
import world.phantasmal.web.questEditor.controllers.QuestEditorToolbarController
import world.phantasmal.webui.dom.Icon
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.*

class QuestEditorToolbarWidget(private val ctrl: QuestEditorToolbarController) : Widget() {
    override fun Node.createElement() =
        div {
            className = "pw-quest-editor-toolbar"

            addChild(Toolbar(
                children = listOf(
                    Button(
                        text = "New quest",
                        iconLeft = Icon.NewFile,
                        onClick = { scope.launch { ctrl.createNewQuest(Episode.I) } },
                    ),
                    FileButton(
                        text = "Open file...",
                        tooltip = value("Open a quest file (Ctrl-O)"),
                        iconLeft = Icon.File,
                        accept = ".bin, .dat, .qst",
                        multiple = true,
                        filesSelected = { files -> scope.launch { ctrl.openFiles(files) } },
                    ),
                    Button(
                        text = "Undo",
                        iconLeft = Icon.Undo,
                        enabled = ctrl.undoEnabled,
                        tooltip = ctrl.undoTooltip,
                        onClick = { ctrl.undo() },
                    ),
                    Button(
                        text = "Redo",
                        iconLeft = Icon.Redo,
                        enabled = ctrl.redoEnabled,
                        tooltip = ctrl.redoTooltip,
                        onClick = { ctrl.redo() },
                    ),
                    Select(
                        enabled = ctrl.areaSelectEnabled,
                        itemsVal = ctrl.areas,
                        itemToString = { it.label },
                        selectedVal = ctrl.currentArea,
                        onSelect = ctrl::setCurrentArea,
                    )
                )
            ))
        }
}