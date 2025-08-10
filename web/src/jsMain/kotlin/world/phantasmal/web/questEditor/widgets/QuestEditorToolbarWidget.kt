package world.phantasmal.web.questEditor.widgets

import kotlinx.coroutines.launch
import org.w3c.dom.Node
import org.w3c.dom.events.KeyboardEvent
import world.phantasmal.cell.cell
import world.phantasmal.cell.list.listCell
import world.phantasmal.cell.map
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.fileFormats.quest.Version
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
                    Dropdown(
                        text = "New quest",
                        iconLeft = Icon.NewFile,
                        items = listCell(Episode.I),
                        itemToString = { "Episode $it" },
                        onSelect = { scope.launch { ctrl.createNewQuest(it) } },
                    ),
                    FileButton(
                        text = "Open file...",
                        tooltip = cell("Open a quest file (Ctrl-O)"),
                        iconLeft = Icon.File,
                        types = ctrl.supportedFileTypes,
                        multiple = true,
                        filesSelected = { files -> scope.launch { ctrl.openFiles(files) } },
                    ),
                    Button(
                        text = "Save",
                        iconLeft = Icon.Save,
                        enabled = ctrl.saveEnabled,
                        tooltip = ctrl.saveTooltip,
                        onClick = { scope.launch { ctrl.save() } },
                    ),
                    Button(
                        text = "Save as...",
                        iconLeft = Icon.Save,
                        enabled = ctrl.saveAsEnabled,
                        tooltip = cell("Save this quest to a new file (Ctrl-Shift-S)"),
                        onClick = { ctrl.saveAs() },
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
                        items = ctrl.areas,
                        itemToString = { it.label },
                        selected = ctrl.currentArea,
                        onSelect = ctrl::setCurrentArea,
                    ),
                    Checkbox(
                        label = "Simple view",
                        tooltip = cell(
                            "Whether the collision or the render geometry should be shown",
                        ),
                        checked = ctrl.showCollisionGeometry,
                        onChange = ctrl::setShowCollisionGeometry,
                    ),
                    Checkbox(
                        label = "Show room IDs",
                        tooltip = cell(
                            "Whether to show room ID numbers in each section",
                        ),
                        checked = ctrl.showRoomIds,
                        onChange = ctrl::setShowRoomIds,
                    ),
                    Checkbox(
                        label = "Spawn monsters on ground",
                        tooltip = cell(
                            "Whether monsters should spawn directly at ground level (section height)",
                        ),
                        checked = ctrl.spawnMonstersOnGround,
                        onChange = ctrl::setSpawnMonstersOnGround,
                    ),
                    Label(
                        textCell = ctrl.mouseWorldPosition.map { position ->
                            if (position != null) {
                                "World: (${position.x.asDynamic().toFixed(1)}, ${
                                    position.y.asDynamic().toFixed(1)
                                }, ${position.z.asDynamic().toFixed(1)})"
                            } else {
                                "World: (--, --, --)"
                            }
                        }
                    ).apply {
                        element.className += " pw-quest-editor-mouse-coordinates"
                    }
                )
            ))

            val saveAsDialog = addDisposable(Dialog(
                visible = ctrl.saveAsDialogVisible,
                title = cell("Save As"),
                content = {
                    div {
                        className = "pw-quest-editor-toolbar-save-as"

                        if (ctrl.showSaveAsDialogNameField) {
                            val filenameInput = TextInput(
                                label = "File name:",
                                value = ctrl.filename,
                                onChange = ctrl::setFilename,
                            )
                            addWidget(filenameInput.label!!)
                            addWidget(filenameInput)
                        }

                        val versionSelect = Select(
                            label = "Version:",
                            items = listCell(Version.GC, Version.BB),
                            selected = ctrl.version,
                            itemToString = {
                                when (it) {
                                    Version.DC -> "Dreamcast"
                                    Version.GC -> "GameCube"
                                    Version.PC -> "PC"
                                    Version.BB -> "BlueBurst"
                                }
                            },
                            onSelect = ctrl::setVersion,
                        )
                        addWidget(versionSelect.label!!)
                        addWidget(versionSelect)
                    }
                },
                footer = {
                    addWidget(Button(
                        text = "Save",
                        onClick = { scope.launch { ctrl.saveAsDialogSave() } },
                    ))
                    addWidget(Button(
                        text = "Cancel",
                        onClick = { ctrl.dismissSaveAsDialog() },
                    ))
                },
                onDismiss = ctrl::dismissSaveAsDialog,
            ))

            saveAsDialog.dialogElement.addEventListener("keydown", { e ->
                if ((e as KeyboardEvent).key == "Enter") {
                    scope.launch { ctrl.saveAsDialogSave() }
                }
            })

            addDisposable(ResultDialog(
                visible = ctrl.resultDialogVisible,
                result = ctrl.result,
                onDismiss = ctrl::dismissResultDialog,
            ))
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-quest-editor-toolbar-save-as {
                    display: grid;
                    grid-template-columns: 100px max-content;
                    grid-column-gap: 4px;
                    grid-row-gap: 4px;
                    align-items: center;
                }

                .pw-quest-editor-toolbar-save-as .pw-input {
                    margin: 1px;
                }

                .pw-quest-editor-mouse-coordinates {
                    font-family: monospace;
                    font-size: 12px;
                    color: #888;
                    white-space: nowrap;
                    margin-left: 8px;
                    padding: 2px 4px;
                    background: rgba(0, 0, 0, 0.1);
                    border-radius: 3px;
                }
            """.trimIndent())
        }
    }
}
