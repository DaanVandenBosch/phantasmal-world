package world.phantasmal.web.questEditor.widgets

import org.w3c.dom.Node
import world.phantasmal.cell.cell
import world.phantasmal.cell.map
import world.phantasmal.web.core.widgets.UnavailableWidget
import world.phantasmal.web.questEditor.controllers.*
import world.phantasmal.webui.dom.*
import world.phantasmal.webui.widgets.*

class MonsterRandomnessWidget(
    private val ctrl: MonsterRandomnessController,
) : Widget() {
    override fun Node.createElement() =
        div {
            className = "pw-quest-editor-monster-randomness"
            tabIndex = -1

            addEventListener("focus", { ctrl.focused() }, true)

            div {
                className = "pw-quest-editor-monster-randomness-inner"
                hidden(ctrl.unavailable)

                addChild(TabContainer(
                    ctrl = ctrl,
                    createWidget = { tab ->
                        when (tab) {
                            MonsterRandomnessTab.MonsterPosition -> monsterPositionTab()
                            MonsterRandomnessTab.EnemyConfiguration -> enemyConfigurationTab()
                        }
                    },
                ))
            }
            addChild(UnavailableWidget(
                visible = ctrl.unavailable,
                message = "No quest loaded.",
            ))
        }

    private fun monsterPositionTab(): Widget = object : Widget() {
        override fun Node.createElement() =
            div {
                className = "pw-quest-editor-mr-tab"

                div {
                    className = "pw-quest-editor-mr-rooms"

                    addChild(Toolbar(
                        children = listOf(
                            Button(
                                enabled = ctrl.enabled,
                                text = "Add room",
                                iconLeft = Icon.Plus,
                                tooltip = cell("Add a new room"),
                                onClick = { ctrl.addRoom() },
                            ),
                            Button(
                                enabled = map(ctrl.enabled, ctrl.selectedRoomIndex) { e, i -> e && i >= 0 },
                                text = "Delete",
                                iconLeft = Icon.Remove,
                                tooltip = cell("Delete selected room"),
                                onClick = { ctrl.deleteRoom() },
                            ),
                        )
                    ))
                    div {
                        className = "pw-quest-editor-mr-room-list"

                        bindChildWidgetsTo(ctrl.rooms) { room, idx ->
                            RoomItemWidget(ctrl, room, idx)
                        }
                    }
                }
                div {
                    className = "pw-quest-editor-mr-detail"

                    addChild(Toolbar(
                        children = listOf(
                            Button(
                                enabled = map(ctrl.enabled, ctrl.selectedRoomIndex) { e, i -> e && i >= 0 },
                                text = "Add entry",
                                iconLeft = Icon.Plus,
                                tooltip = cell("Add a new spawn entry"),
                                onClick = { ctrl.addSpawnEntry() },
                            ),
                        )
                    ))
                    div {
                        className = "pw-quest-editor-mr-table-container"

                        table {
                            className = "pw-quest-editor-mr-table"

                            thead {
                                tr {
                                    th { textContent = "#" }
                                    th { textContent = "Pos X" }
                                    th { textContent = "Pos Y" }
                                    th { textContent = "Pos Z" }
                                    th { textContent = "Rot. X" }
                                    th { textContent = "Rot. Y" }
                                    th { textContent = "Rot. Z" }
                                    th { textContent = "Room ID" }
                                    th { textContent = "Entry #" }
                                    th { }
                                }
                            }
                            tbody {
                                bindChildWidgetsTo(ctrl.selectedRoomEntries) { indexed, _ ->
                                    SpawnEntryRowWidget(ctrl, indexed)
                                }
                            }
                        }
                    }
                }
            }
    }

    private fun enemyConfigurationTab(): Widget = object : Widget() {
        override fun Node.createElement() =
            div {
                className = "pw-quest-editor-mr-enemy-config"

                // Config pool section (Table 5A).
                div {
                    className = "pw-quest-editor-mr-section"

                    div {
                        className = "pw-quest-editor-mr-section-header"
                        span { textContent = "Config pool" }
                        addWidget(Button(
                            enabled = ctrl.enabled,
                            text = "Add",
                            iconLeft = Icon.Plus,
                            tooltip = cell("Add config pool entry"),
                            onClick = { ctrl.addConfigPoolEntry() },
                        ))
                    }
                    div {
                        className = "pw-quest-editor-mr-table-container"

                        table {
                            className = "pw-quest-editor-mr-table"

                            thead {
                                tr {
                                    th { textContent = "#" }
                                    th { textContent = "Base X" }
                                    th { textContent = "Base Z" }
                                    th { textContent = "Base Y" }
                                    th { textContent = "Float: unkn" }
                                    th { textContent = "32b: unkn" }
                                    th { textContent = "16b: unkn" }
                                    th { textContent = "16b: unkn" }
                                    th { textContent = "Config #" }
                                    th { textContent = "16b: unkn" }
                                    th { }
                                }
                            }
                            tbody {
                                bindChildWidgetsTo(ctrl.configPoolEntries) { indexed, _ ->
                                    ConfigPoolEntryRowWidget(ctrl, indexed)
                                }
                            }
                        }
                    }
                }

                // Monsters setting section (Table 5B).
                div {
                    className = "pw-quest-editor-mr-section"

                    div {
                        className = "pw-quest-editor-mr-section-header"
                        span { textContent = "Monsters setting" }
                        addWidget(Button(
                            enabled = ctrl.enabled,
                            text = "Add",
                            iconLeft = Icon.Plus,
                            tooltip = cell("Add monster setting entry"),
                            onClick = { ctrl.addMappingEntry() },
                        ))
                    }
                    div {
                        className = "pw-quest-editor-mr-table-container"

                        table {
                            className = "pw-quest-editor-mr-table"

                            thead {
                                tr {
                                    th { textContent = "#" }
                                    th { textContent = "Monster Type" }
                                    th { textContent = "Config ID" }
                                    th { textContent = "Ratio" }
                                    th { }
                                }
                            }
                            tbody {
                                bindChildWidgetsTo(ctrl.monsterSettingEntries) { indexed, _ ->
                                    MappingEntryRowWidget(ctrl, indexed)
                                }
                            }
                        }
                    }
                }
            }
    }

    companion object {
        init {
            @Suppress("CssUnusedSymbol", "CssUnresolvedCustomProperty")
            // language=css
            style("""
                .pw-quest-editor-monster-randomness {
                    overflow: hidden;
                    outline: none;
                    height: 100%;
                }

                .pw-quest-editor-monster-randomness-inner {
                    display: flex;
                    flex-direction: column;
                    width: 100%;
                    height: 100%;
                    overflow: hidden;
                }

                .pw-quest-editor-monster-randomness-inner > .pw-tab-container {
                    flex: 1;
                    overflow: hidden;
                }

                .pw-quest-editor-mr-tab {
                    display: flex;
                    flex-direction: row;
                    width: 100%;
                    height: 100%;
                    overflow: hidden;
                }

                .pw-quest-editor-mr-rooms {
                    flex: 0 0 200px;
                    display: flex;
                    flex-direction: column;
                    border-right: var(--pw-border);
                    overflow: hidden;
                }

                .pw-quest-editor-mr-room-list {
                    flex: 1;
                    overflow-y: auto;
                }

                .pw-quest-editor-mr-room-item {
                    display: flex;
                    align-items: center;
                    padding: 4px 8px;
                    cursor: pointer;
                    font-size: 12px;
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                }

                .pw-quest-editor-mr-room-item .pw-number-input {
                    width: 50px;
                    margin: 0 2px;
                }

                .pw-quest-editor-mr-room-item:hover {
                    background-color: hsl(0, 0%, 25%);
                }

                .pw-quest-editor-mr-room-item.pw-selected {
                    background-color: hsl(0, 0%, 30%);
                    color: hsl(0, 0%, 95%);
                }

                .pw-quest-editor-mr-detail {
                    flex: 1;
                    display: flex;
                    flex-direction: column;
                    overflow: hidden;
                }

                .pw-quest-editor-mr-enemy-config {
                    display: flex;
                    flex-direction: column;
                    width: 100%;
                    height: 100%;
                    overflow: hidden;
                }

                .pw-quest-editor-mr-section {
                    display: flex;
                    flex-direction: column;
                    min-height: 0;
                }

                .pw-quest-editor-mr-section:first-child {
                    flex: 1;
                    overflow: hidden;
                }

                .pw-quest-editor-mr-section:last-child {
                    flex: 1;
                    overflow: hidden;
                }

                .pw-quest-editor-mr-section-header {
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    padding: 4px 8px;
                    font-size: 13px;
                    font-weight: bold;
                    border-bottom: var(--pw-border);
                    background-color: hsl(0, 0%, 15%);
                }

                .pw-quest-editor-mr-section-header > span {
                    flex: 1;
                }

                .pw-quest-editor-mr-table-container {
                    flex: 1;
                    overflow: auto;
                }

                .pw-quest-editor-mr-table {
                    width: 100%;
                    border-collapse: collapse;
                    font-size: 12px;
                }

                .pw-quest-editor-mr-table th {
                    position: sticky;
                    top: 0;
                    background-color: hsl(0, 0%, 18%);
                    text-align: left;
                    padding: 4px 6px;
                    white-space: nowrap;
                    border-bottom: var(--pw-border);
                    z-index: 1;
                }

                .pw-quest-editor-mr-table td {
                    padding: 2px 4px;
                    border-bottom: 1px solid hsl(0, 0%, 20%);
                }

                .pw-quest-editor-mr-table .pw-number-input {
                    width: 70px;
                }

                .pw-quest-editor-mr-table .pw-combobox {
                    width: 180px;
                }
            """.trimIndent())
        }
    }
}

private class SpawnEntryRowWidget(
    private val ctrl: MonsterRandomnessController,
    private val indexed: IndexedSpawnEntry,
) : Widget() {
    override fun Node.createElement() =
        tr {
            val entry = indexed.entry
            val idx = indexed.index
            val rev = ctrl.cmDataRevision

            td { textContent = "${idx + 1}" }
            td {
                addWidget(DoubleInput(
                    enabled = ctrl.enabled,
                    value = rev.map { entry.x.toDouble() },
                    onChange = { ctrl.setSpawnField(idx) { e -> e.x = it.toFloat() } },
                    roundTo = 4,
                ))
            }
            td {
                addWidget(DoubleInput(
                    enabled = ctrl.enabled,
                    value = rev.map { entry.unknown1.toDouble() },
                    onChange = { ctrl.setSpawnField(idx) { e -> e.unknown1 = it.toFloat() } },
                    roundTo = 4,
                ))
            }
            td {
                addWidget(DoubleInput(
                    enabled = ctrl.enabled,
                    value = rev.map { entry.y.toDouble() },
                    onChange = { ctrl.setSpawnField(idx) { e -> e.y = it.toFloat() } },
                    roundTo = 4,
                ))
            }
            td {
                addWidget(DoubleInput(
                    enabled = ctrl.enabled,
                    value = rev.map { entry.unknown2.toDouble() },
                    onChange = { ctrl.setSpawnField(idx) { e -> e.unknown2 = it.toFloat() } },
                    roundTo = 4,
                ))
            }
            td {
                addWidget(IntInput(
                    enabled = ctrl.enabled,
                    value = rev.map { entry.rotation.toInt() },
                    onChange = { ctrl.setSpawnField(idx) { e -> e.rotation = it.toShort() } },
                ))
            }
            td {
                addWidget(IntInput(
                    enabled = ctrl.enabled,
                    value = rev.map { entry.unknown3.toInt() },
                    onChange = { ctrl.setSpawnField(idx) { e -> e.unknown3 = it.toShort() } },
                ))
            }
            td {
                addWidget(IntInput(
                    enabled = ctrl.enabled,
                    value = rev.map { entry.sectionId.toInt() },
                    onChange = { ctrl.setSpawnField(idx) { e -> e.sectionId = it.toShort() } },
                ))
            }
            td {
                addWidget(IntInput(
                    enabled = ctrl.enabled,
                    value = rev.map { entry.unknown5.toInt() },
                    onChange = { ctrl.setSpawnField(idx) { e -> e.unknown5 = it.toShort() } },
                ))
            }
            td {
                addWidget(Button(
                    enabled = ctrl.enabled,
                    iconLeft = Icon.Remove,
                    tooltip = cell("Delete this entry"),
                    onClick = { ctrl.deleteSpawnEntry(idx) },
                ))
            }
        }
}

private class ConfigPoolEntryRowWidget(
    private val ctrl: MonsterRandomnessController,
    private val indexed: IndexedConfigPoolEntry,
) : Widget() {
    override fun Node.createElement() =
        tr {
            val entry = indexed.entry
            val idx = indexed.index
            val rev = ctrl.cmDataRevision

            td { textContent = "${idx + 1}" }
            td {
                addWidget(DoubleInput(
                    enabled = ctrl.enabled,
                    value = rev.map { entry.baseX.toDouble() },
                    onChange = { ctrl.setConfigPoolField(idx) { e -> e.baseX = it.toFloat() } },
                    roundTo = 4,
                ))
            }
            td {
                addWidget(DoubleInput(
                    enabled = ctrl.enabled,
                    value = rev.map { entry.baseZ.toDouble() },
                    onChange = { ctrl.setConfigPoolField(idx) { e -> e.baseZ = it.toFloat() } },
                    roundTo = 4,
                ))
            }
            td {
                addWidget(DoubleInput(
                    enabled = ctrl.enabled,
                    value = rev.map { entry.baseY.toDouble() },
                    onChange = { ctrl.setConfigPoolField(idx) { e -> e.baseY = it.toFloat() } },
                    roundTo = 4,
                ))
            }
            td {
                addWidget(DoubleInput(
                    enabled = ctrl.enabled,
                    value = rev.map { entry.unknownFloat.toDouble() },
                    onChange = { ctrl.setConfigPoolField(idx) { e -> e.unknownFloat = it.toFloat() } },
                    roundTo = 4,
                ))
            }
            td {
                addWidget(IntInput(
                    enabled = ctrl.enabled,
                    value = rev.map { entry.unknownDword },
                    onChange = { ctrl.setConfigPoolField(idx) { e -> e.unknownDword = it } },
                ))
            }
            td {
                addWidget(IntInput(
                    enabled = ctrl.enabled,
                    value = rev.map { entry.unknownWord1.toInt() },
                    onChange = { ctrl.setConfigPoolField(idx) { e -> e.unknownWord1 = it.toShort() } },
                ))
            }
            td {
                addWidget(IntInput(
                    enabled = ctrl.enabled,
                    value = rev.map { entry.unknownWord2.toInt() },
                    onChange = { ctrl.setConfigPoolField(idx) { e -> e.unknownWord2 = it.toShort() } },
                ))
            }
            td {
                addWidget(IntInput(
                    enabled = ctrl.enabled,
                    value = rev.map { entry.configId },
                    onChange = { ctrl.setConfigPoolField(idx) { e -> e.configId = it } },
                ))
            }
            td {
                addWidget(IntInput(
                    enabled = ctrl.enabled,
                    value = rev.map { entry.unknownWord3.toInt() },
                    onChange = { ctrl.setConfigPoolField(idx) { e -> e.unknownWord3 = it.toShort() } },
                ))
            }
            td {
                addWidget(Button(
                    enabled = ctrl.enabled,
                    iconLeft = Icon.Remove,
                    tooltip = cell("Delete this entry"),
                    onClick = { ctrl.deleteConfigPoolEntry(idx) },
                ))
            }
        }
}

private class RoomItemWidget(
    private val ctrl: MonsterRandomnessController,
    private val room: RoomInfo,
    private val index: Int,
) : Widget() {
    override fun Node.createElement() =
        div {
            className = "pw-quest-editor-mr-room-item"

            toggleClass("pw-selected", ctrl.selectedRoomIndex.map { it == index })

            onclick = { ctrl.selectRoom(index) }

            span { textContent = "Room " }
            addWidget(IntInput(
                enabled = ctrl.enabled,
                value = ctrl.cmDataRevision.map { room.roomId },
                onChange = { ctrl.setRoomId(room.globalIndex, it) },
                min = 0,
            ))
            span { textContent = " (${room.entryCount} entries)" }
        }
}

private class MappingEntryRowWidget(
    private val ctrl: MonsterRandomnessController,
    private val indexed: IndexedMappingEntry,
) : Widget() {
    override fun Node.createElement() =
        tr {
            val entry = indexed.entry
            val idx = indexed.index
            val rev = ctrl.cmDataRevision

            td { textContent = "${idx + 1}" }
            td {
                addWidget(ComboBox(
                    enabled = ctrl.enabled,
                    items = cell(ctrl.monsterTypeOptions),
                    itemToString = { "${it.name} - ${it.index}" },
                    selected = rev.map {
                        val typeIdx = entry.monsterTypeIndex.toInt() and 0xFF
                        ctrl.monsterTypeOptions.find { opt -> opt.index == typeIdx }
                    },
                    onSelect = { ctrl.setMappingField(idx) { e -> e.monsterTypeIndex = it.index.toByte() } },
                ))
            }
            td {
                addWidget(IntInput(
                    enabled = ctrl.enabled,
                    value = rev.map { entry.configId.toInt() and 0xFF },
                    onChange = { ctrl.setMappingField(idx) { e -> e.configId = it.toByte() } },
                    min = 0,
                    max = 255,
                ))
            }
            td {
                addWidget(IntInput(
                    enabled = ctrl.enabled,
                    value = rev.map { entry.spawnRatio.toInt() and 0xFFFF },
                    onChange = { ctrl.setMappingField(idx) { e -> e.spawnRatio = it.toShort() } },
                    min = 0,
                    max = 65535,
                ))
            }
            td {
                addWidget(Button(
                    enabled = ctrl.enabled,
                    iconLeft = Icon.Remove,
                    tooltip = cell("Delete this entry"),
                    onClick = { ctrl.deleteMappingEntry(idx) },
                ))
            }
        }
}
