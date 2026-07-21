package world.phantasmal.web.questEditor.widgets

import kotlinx.coroutines.launch
import org.w3c.dom.Node
import org.w3c.dom.events.KeyboardEvent
import world.phantasmal.cell.cell
import world.phantasmal.cell.list.listCell
import world.phantasmal.cell.mutableCell
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.fileFormats.quest.Version
import world.phantasmal.web.questEditor.controllers.CompatibilityController
import world.phantasmal.web.questEditor.controllers.QuestEditorToolbarController
import world.phantasmal.web.questEditor.controllers.SaveFormat
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.files.showOpenFilePicker
import world.phantasmal.webui.widgets.*

class QuestEditorToolbarWidget(
    private val ctrl: QuestEditorToolbarController,
    private val compatibilityCtrl: CompatibilityController,
) : Widget() {
    private val compatibilityDialogVisible = mutableCell(false)
    private val aboutDialogVisible = mutableCell(false)

    override fun Node.createElement() =
        div {
            className = "pw-quest-editor-toolbar"


            addChild(Toolbar(
                children = listOf(
                    // File Menu
                    ToolbarMenu(
                        text = "File",
                        items = listOf(
                            MenuItem.SubMenu(
                                label = "New Quest",
                                items = listOf(
                                    MenuItem.Action(
                                        label = "Episode I",
                                        onAction = { scope.launch { ctrl.createNewQuest(Episode.I) } },
                                    ),
                                    MenuItem.Action(
                                        label = "Episode II",
                                        onAction = { scope.launch { ctrl.createNewQuest(Episode.II) } },
                                    ),
                                    MenuItem.Action(
                                        label = "Episode IV",
                                        onAction = { scope.launch { ctrl.createNewQuest(Episode.IV) } },
                                    ),
                                ),
                            ),
                            MenuItem.SubMenu(
                                label = "Open Lobby",
                                visible = ctrl.showLobbyMenu,
                                items = (1..10).map { v ->
                                    MenuItem.Action(
                                        label = "Lobby ${v.toString().padStart(2, '0')}",
                                        onAction = { scope.launch { ctrl.loadLobbyQuest(v) } },
                                    )
                                } + listOf(
                                    MenuItem.Separator,
                                    MenuItem.Action(
                                        label = "EP3 Green",
                                        onAction = { scope.launch { ctrl.loadLobbyQuest(11) } },
                                    ),
                                    MenuItem.Action(
                                        label = "EP3 Red",
                                        onAction = { scope.launch { ctrl.loadLobbyQuest(12) } },
                                    ),
                                    MenuItem.Action(
                                        label = "EP3 Yellow",
                                        onAction = { scope.launch { ctrl.loadLobbyQuest(13) } },
                                    ),
                                    MenuItem.Separator,
                                    MenuItem.Action(
                                        label = "Soccer 1",
                                        onAction = { scope.launch { ctrl.loadLobbyQuest(14) } },
                                    ),
                                    MenuItem.Action(
                                        label = "Soccer 2",
                                        onAction = { scope.launch { ctrl.loadLobbyQuest(15) } },
                                    ),
                                ),
                            ),
                            MenuItem.Action(
                                label = "Open File...",
                                shortcut = "Ctrl+O",
                                onAction = {
                                    scope.launch {
                                        ctrl.openFiles(
                                            showOpenFilePicker(ctrl.supportedFileTypes, multiple = true)
                                        )
                                    }
                                },
                            ),
                            MenuItem.Separator,
                            MenuItem.Action(
                                label = "Save",
                                shortcut = "Ctrl+S",
                                enabled = ctrl.saveEnabled,
                                onAction = { scope.launch { ctrl.save() } },
                            ),
                            MenuItem.Action(
                                label = "Save As...",
                                shortcut = "Ctrl+Shift+S",
                                enabled = ctrl.saveAsEnabled,
                                onAction = { ctrl.saveAs() },
                            ),
                        ),
                    ),
                    // View Menu
                    ToolbarMenu(
                        text = "View",
                        items = listOf(
                            MenuItem.Check(
                                label = "Simple View",
                                tooltip = "Whether the collision or the render geometry should be shown",
                                checked = ctrl.showCollisionGeometry,
                                onChange = ctrl::setShowCollisionGeometry,
                            ),
                            MenuItem.Separator,
                            MenuItem.Check(
                                label = "Section IDs",
                                tooltip = "Whether to show section ID numbers in each section",
                                checked = ctrl.showSectionIds,
                                onChange = ctrl::setShowSectionIds,
                            ),
                            MenuItem.Check(
                                label = "Door & Fence IDs",
                                tooltip = "Whether to show door and fence ID labels",
                                checked = ctrl.showDoorIds,
                                onChange = ctrl::setShowDoorIds,
                            ),
                            MenuItem.Check(
                                label = "Spawn Ground",
                                tooltip = "Whether monsters should spawn directly at ground level (section height)",
                                checked = ctrl.spawnMonstersOnGround,
                                onChange = ctrl::setSpawnMonstersOnGround,
                            ),
                            MenuItem.Check(
                                label = "Origin Point",
                                tooltip = "Show the world coordinate origin point at position (0,0,0)",
                                checked = ctrl.showOriginPoint,
                                onChange = ctrl::setShowOriginPoint,
                            ),
                            MenuItem.Check(
                                label = "Quest Particles",
                                tooltip = "Show fixed DAT object and BIN opcode particle effects",
                                checked = ctrl.showQuestParticles,
                                onChange = ctrl::setShowQuestParticles,
                            ),
                            MenuItem.Separator,
                            MenuItem.Check(
                                label = "City Map",
                                tooltip = "Toggle between default and city map for the current episode",
                                checked = ctrl.showCityMap,
                                onChange = { scope.launch { ctrl.setShowCityMap(it) } },
                            ),
                        ),
                    ),
                    // Tools Menu
                    ToolbarMenu(
                        text = "Tools",
                        items = listOf(
                            MenuItem.Action(
                                label = "Compatibility Check",
                                onAction = { compatibilityDialogVisible.value = true },
                            ),
                            MenuItem.Separator,
                            MenuItem.Action(
                                label = "About",
                                onAction = { aboutDialogVisible.value = true },
                            ),
                        ),
                    ),
                    // Undo
                    Button(
                        text = "Undo",
                        iconLeft = world.phantasmal.webui.dom.Icon.Undo,
                        enabled = ctrl.undoEnabled,
                        tooltip = ctrl.undoTooltip,
                        onClick = { ctrl.undo() },
                    ),
                    // Redo
                    Button(
                        text = "Redo",
                        iconLeft = world.phantasmal.webui.dom.Icon.Redo,
                        enabled = ctrl.redoEnabled,
                        tooltip = ctrl.redoTooltip,
                        onClick = { ctrl.redo() },
                    ),
                    // Area selector
                    Select(
                        enabled = ctrl.areaSelectEnabled,
                        items = ctrl.areas,
                        itemToString = { it.label },
                        selected = ctrl.currentArea,
                        onSelect = ctrl::setCurrentArea,
                    ),
                    // Goto Section (moved after Area selector)
                    ComboBox(
                        className = "pw-goto-section",
                        label = "Goto Section:",
                        enabled = ctrl.gotoSectionEnabled,
                        items = ctrl.filteredSections,
                        itemToString = { "${it.id}" },
                        selected = ctrl.selectedSection,
                        onSelect = { ctrl.selectSection(it) },
                        filter = { ctrl.filterSections(it) },
                    ),
                    DoubleInput(
                        label = "Goto X:",
                        enabled = ctrl.gotoWorldPositionEnabled,
                        value = ctrl.gotoWorldX,
                        onChange = ctrl::setGotoWorldX,
                        roundTo = -1,
                        onPaste = ctrl::smartPasteGotoPosition,
                    ),
                    DoubleInput(
                        label = "Y:",
                        enabled = ctrl.gotoWorldPositionEnabled,
                        value = ctrl.gotoWorldY,
                        onChange = ctrl::setGotoWorldY,
                        roundTo = -1,
                        onPaste = ctrl::smartPasteGotoPosition,
                    ),
                    DoubleInput(
                        label = "Z:",
                        enabled = ctrl.gotoWorldPositionEnabled,
                        value = ctrl.gotoWorldZ,
                        onChange = ctrl::setGotoWorldZ,
                        roundTo = -1,
                        onPaste = ctrl::smartPasteGotoPosition,
                    ),
                    // Free roam variant controls
                    Select(
                        className = "pw-free-roam-select",
                        label = "Layout:",
                        visible = ctrl.showFreeRoamV1,
                        items = ctrl.freeRoamV1Options,
                        itemToString = { it.toString() },
                        selected = ctrl.freeRoamV1,
                        onSelect = { scope.launch { ctrl.setFreeRoamV1(it) } },
                    ),
                    Select(
                        className = "pw-free-roam-select",
                        label = "Monsters:",
                        visible = ctrl.showFreeRoamV2,
                        items = ctrl.freeRoamV2Options,
                        itemToString = { it.toString() },
                        selected = ctrl.freeRoamV2,
                        onSelect = { scope.launch { ctrl.setFreeRoamV2(it) } },
                    ),
                    Checkbox(
                        label = "Offline",
                        visible = ctrl.showFreeRoamOnOff,
                        checked = ctrl.freeRoamOffline,
                        onChange = { scope.launch { ctrl.setFreeRoamOffline(it) } },
                    ),
                    Checkbox(
                        label = "Ultimate",
                        visible = ctrl.showFreeRoamUltimate,
                        checked = ctrl.freeRoamUltimate,
                        onChange = { scope.launch { ctrl.setFreeRoamUltimate(it) } },
                    ),
                )
            ))

            // Save As Dialog
            val saveAsDialog = addDisposable(Dialog(
                visible = ctrl.saveAsDialogVisible,
                title = cell("Save As"),
                content = {
                    div {
                        className = "pw-quest-editor-toolbar-save-as"

                        val formatSelect = Select(
                            label = "Format:",
                            items = listCell(*ctrl.availableSaveFormats.toTypedArray()),
                            selected = ctrl.saveFormat,
                            itemToString = {
                                when (it) {
                                    SaveFormat.QST -> "QST (.qst)"
                                    SaveFormat.BIN_DAT -> "BIN + DAT"
                                    SaveFormat.FREE_ROAM -> "Free Roam (Directory)"
                                }
                            },
                            onSelect = ctrl::setSaveFormat,
                        )
                        addWidget(formatSelect.label!!)
                        addWidget(formatSelect)

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

                        val compressedCheckbox = Checkbox(
                            visible = ctrl.compressedVisible,
                            label = "PRS Compressed",
                            checked = ctrl.compressed,
                            onChange = ctrl::setCompressed,
                        )
                        addWidget(compressedCheckbox)
                        addWidget(compressedCheckbox.label!!)
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

            addDisposable(
                CompatibilityDialog(
                    visible = compatibilityDialogVisible,
                    ctrl = compatibilityCtrl,
                    onDismiss = { compatibilityDialogVisible.value = false },
                )
            )

            addDisposable(
                AboutDialog(
                    visible = aboutDialogVisible,
                    onDismiss = { aboutDialogVisible.value = false },
                )
            )
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-goto-section {
                    width: 40px;
                }

                /*
                 * Browsers give <input> a default min-width based on its size attribute (~150px),
                 * which makes the inner input overflow the 40px container — clicks land on it
                 * instead of whichever toolbar widget visually sits to the right. Force the input
                 * to honor its flex parent so the click target is bounded by the 40px box.
                 */
                .pw-goto-section .pw-combobox-inner > input {
                    width: 0;
                    min-width: 0;
                }

                .pw-goto-section .pw-combobox-button {
                    display: none;
                }

                .pw-free-roam-select {
                    width: 60px;
                }

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

            """.trimIndent())
        }
    }
}
