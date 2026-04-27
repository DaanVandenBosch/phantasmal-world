package world.phantasmal.web.questEditor.widgets

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import world.phantasmal.cell.Cell
import world.phantasmal.cell.cell
import world.phantasmal.cell.map
import world.phantasmal.cell.mutableCell
import world.phantasmal.cell.observeNow
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.psolib.fileFormats.quest.ObjectType
import world.phantasmal.web.externals.three.Vector3
import world.phantasmal.web.questEditor.controllers.AreaNpcListController
import world.phantasmal.web.questEditor.controllers.AreaObjectListController
import world.phantasmal.web.questEditor.controllers.MonsterRandomnessController
import world.phantasmal.web.questEditor.loading.SymbolChatColliRepository
import world.phantasmal.web.questEditor.models.QuestObjectModel
import world.phantasmal.web.questEditor.rendering.QuestRenderer
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.web.questEditor.stores.QuestEditorUiStore
import world.phantasmal.web.questEditor.stores.ViewportStore
import world.phantasmal.webui.dom.disposableListener
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.dom.dom
import world.phantasmal.webui.dom.span
import world.phantasmal.webui.widgets.Button
import world.phantasmal.webui.widgets.Dialog

class QuestEditorRendererWidget(
    renderer: QuestRenderer,
    mouseWorldPosition: Cell<Vector3?>,
    playbackActionText: Cell<String>,
    private val questEditorStore: QuestEditorStore,
    private val questEditorUiStore: QuestEditorUiStore,
    private val viewportStore: ViewportStore,
    private val monsterRandomnessCtrl: MonsterRandomnessController,
    private val areaNpcListCtrl: AreaNpcListController,
    private val areaObjectListCtrl: AreaObjectListController,
    private val symbolChatColliRepository: SymbolChatColliRepository,
) : QuestRendererWidget(renderer, mouseWorldPosition, playbackActionText) {

    private val monsterRandomnessDialogVisible = mutableCell(false)
    private val editSymbolChatPopupVisible = mutableCell(false)
    private var contextMenuPopup: HTMLDivElement? = null
    private var documentMouseDownListener: Disposable? = null
    private var documentKeyDownListener: Disposable? = null

    override fun interceptElement(element: HTMLElement) {
        // Whether the current selection is a SymbolChatObject
        val isSymbolChatObjectSelected: Cell<Boolean> =
            questEditorStore.selectedEntity.map {
                it is QuestObjectModel && it.type == ObjectType.SymbolChatObject
            }

        // Whether the current area/variant has any CM data
        val hasCmData: Cell<Boolean> = map(
            questEditorStore.currentQuest,
            questEditorStore.currentArea,
            questEditorStore.currentAreaVariant,
        ) { quest, area, areaVariant ->
            if (quest == null || area == null) {
                false
            } else if (quest.floorMappings.isNotEmpty()) {
                val floorIds = quest.floorMappings
                    .filter { it.areaId == area.id && (areaVariant == null || it.variantId == areaVariant.id) }
                    .map { it.floorId }
                    .toSet()
                quest.cmRandomSpawns.value.any { it.areaId in floorIds } ||
                    quest.cmMonsterMappings.value.any { it.areaId in floorIds } ||
                    quest.cmConfigPool.value.any { it.areaId in floorIds }
            } else {
                quest.cmRandomSpawns.value.any { it.areaId == area.id } ||
                    quest.cmMonsterMappings.value.any { it.areaId == area.id } ||
                    quest.cmConfigPool.value.any { it.areaId == area.id }
            }
        }

        // Build context menu popup (appended to body, positioned fixed)
        val popup = dom {
            div {
                className = "pw-toolbar-menu-popup pw-quest-editor-context-menu"
                tabIndex = -1
                style.position = "fixed"
                style.zIndex = "1001"
                style.display = "none"

                // Section IDs toggle
                div {
                    className = "pw-toolbar-menu-item pw-toolbar-menu-item-check"
                    title = "Whether to show section ID numbers in each section"

                    val checkIcon = span {
                        className = "pw-toolbar-menu-item-check-icon"
                    }
                    span {
                        className = "pw-toolbar-menu-item-label"
                        textContent = "Section IDs"
                    }

                    observeNow(questEditorUiStore.showSectionIds) { checked ->
                        checkIcon.textContent = if (checked) "\u2713" else ""
                    }

                    onclick = { e ->
                        e.stopPropagation()
                        questEditorUiStore.setShowSectionIds(!questEditorUiStore.showSectionIds.value)
                    }
                }

                // Door & Fence IDs toggle
                div {
                    className = "pw-toolbar-menu-item pw-toolbar-menu-item-check"
                    title = "Whether to show door and fence ID labels"

                    val checkIcon = span {
                        className = "pw-toolbar-menu-item-check-icon"
                    }
                    span {
                        className = "pw-toolbar-menu-item-label"
                        textContent = "Door & Fence IDs"
                    }

                    observeNow(questEditorUiStore.showDoorIds) { checked ->
                        checkIcon.textContent = if (checked) "\u2713" else ""
                    }

                    onclick = { e ->
                        e.stopPropagation()
                        questEditorUiStore.setShowDoorIds(!questEditorUiStore.showDoorIds.value)
                    }
                }

                // Spawn Ground toggle
                div {
                    className = "pw-toolbar-menu-item pw-toolbar-menu-item-check"
                    title = "Whether monsters should spawn directly at ground level (section height)"

                    val checkIcon = span {
                        className = "pw-toolbar-menu-item-check-icon"
                    }
                    span {
                        className = "pw-toolbar-menu-item-label"
                        textContent = "Spawn Ground"
                    }

                    observeNow(questEditorUiStore.spawnMonstersOnGround) { checked ->
                        checkIcon.textContent = if (checked) "\u2713" else ""
                    }

                    onclick = { e ->
                        e.stopPropagation()
                        questEditorUiStore.setSpawnMonstersOnGround(!questEditorUiStore.spawnMonstersOnGround.value)
                    }
                }

                // Origin Point toggle
                div {
                    className = "pw-toolbar-menu-item pw-toolbar-menu-item-check"
                    title = "Show the world coordinate origin point at position (0,0,0)"

                    val checkIcon = span {
                        className = "pw-toolbar-menu-item-check-icon"
                    }
                    span {
                        className = "pw-toolbar-menu-item-label"
                        textContent = "Origin Point (0,0,0)"
                    }

                    observeNow(questEditorUiStore.showOriginPoint) { checked ->
                        checkIcon.textContent = if (checked) "\u2713" else ""
                    }

                    onclick = { e ->
                        e.stopPropagation()
                        questEditorUiStore.setShowOriginPoint(!questEditorUiStore.showOriginPoint.value)
                    }
                }

                // Separator shown when any contextual action below is visible.
                val hasContextualAction: Cell<Boolean> =
                    map(hasCmData, isSymbolChatObjectSelected) { cm, sc -> cm || sc }
                div {
                    className = "pw-toolbar-menu-separator"
                    observeNow(hasContextualAction) { hidden = !it }
                }

                // Monster Randomness... action (only visible when quest has CM data)
                div {
                    className = "pw-toolbar-menu-item"
                    observeNow(hasCmData) { hidden = !it }

                    span {
                        className = "pw-toolbar-menu-item-label"
                        textContent = "Monster Randomness..."
                    }

                    onclick = {
                        viewportStore.dismissContextMenu()
                        monsterRandomnessDialogVisible.value = true
                    }
                }

                // Edit symbol chat... action (only visible when a SymbolChatObject is selected)
                div {
                    className = "pw-toolbar-menu-item"
                    observeNow(isSymbolChatObjectSelected) { hidden = !it }

                    span {
                        className = "pw-toolbar-menu-item-label"
                        textContent = "Edit symbol chat..."
                    }

                    onclick = {
                        viewportStore.dismissContextMenu()
                        editSymbolChatPopupVisible.value = true
                    }
                }
            }
        }

        contextMenuPopup = popup
        window.document.body?.appendChild(popup)

        // Observe context menu request to show/hide popup
        observeNow(viewportStore.contextMenuRequest) { request ->
            if (request != null) {
                popup.style.left = "${request.clientX}px"
                popup.style.top = "${request.clientY}px"
                popup.style.display = "block"

                // Listen for outside clicks to dismiss
                documentMouseDownListener?.dispose()
                documentMouseDownListener = document.disposableListener("mousedown", { e: Event ->
                    val target = e.target
                    if (target !is org.w3c.dom.Node || !popup.contains(target)) {
                        viewportStore.dismissContextMenu()
                    }
                })

                // Listen for ESC to dismiss
                documentKeyDownListener?.dispose()
                documentKeyDownListener = document.disposableListener("keydown", { e: Event ->
                    if ((e as KeyboardEvent).key == "Escape") {
                        viewportStore.dismissContextMenu()
                    }
                })
            } else {
                popup.style.display = "none"
                documentMouseDownListener?.dispose()
                documentMouseDownListener = null
                documentKeyDownListener?.dispose()
                documentKeyDownListener = null
            }
        }

        // Monster Randomness Dialog
        val mrDialogWidth = 900
        val mrDialogHeight = 700

        val mrDialog = addDisposable(Dialog(
            visible = monsterRandomnessDialogVisible,
            title = cell("Monster Randomness"),
            content = {
                addWidget(MonsterRandomnessWidget(monsterRandomnessCtrl))
            },
            footer = {
                addWidget(Button(
                    text = "Close",
                    onClick = { monsterRandomnessDialogVisible.value = false },
                ))
            },
            onDismiss = { monsterRandomnessDialogVisible.value = false },
        ))
        mrDialog.dialogElement.style.width = "${mrDialogWidth}px"
        mrDialog.dialogElement.style.height = "${mrDialogHeight}px"

        // Make the dialog body fill available space with hidden overflow so inner widget can scroll
        val bodyEl = mrDialog.dialogElement.querySelector(".pw-dialog-body") as? HTMLElement
        bodyEl?.let {
            it.style.setProperty("overflow", "hidden")
            it.style.setProperty("min-height", "0")
        }

        // Re-center the dialog after Dialog's own positioning (which uses hardcoded 500x500)
        observeNow(monsterRandomnessDialogVisible) { visible ->
            if (visible) {
                val x = (window.innerWidth - mrDialogWidth) / 2
                val y = maxOf(20, (window.innerHeight - mrDialogHeight) / 2)
                mrDialog.dialogElement.style.transform = "translate(${x}px, ${y}px)"
            }
        }

        // Symbol Chat Edit Popup (triggered by the context menu action).
        // The popup handles its own sizing and centering internally.
        addDisposable(SymbolChatEditPopup(
            visible = editSymbolChatPopupVisible,
            questEditorStore = questEditorStore,
            symbolChatColliRepository = symbolChatColliRepository,
            onDismiss = { editSymbolChatPopupVisible.value = false },
        ))

        // --- Area NPC/Object overlay panels (slide-in from right edge on hover) ---
        val overlayContainer = dom {
            div {
                className = "pw-quest-editor-overlay-panels"

                // NPC panel
                div {
                    className = "pw-quest-editor-overlay-panel"

                    div {
                        className = "pw-quest-editor-overlay-panel-header"
                        span {
                            className = "pw-quest-editor-overlay-panel-title"
                            observeNow(areaNpcListCtrl.npcs) { npcs ->
                                textContent = "NPCs (${npcs.size})"
                            }
                        }
                    }

                    val npcBody = div {
                        className = "pw-quest-editor-overlay-panel-body"

                        bindDisposableChildrenTo(areaNpcListCtrl.npcs) { npc, _ ->
                            val disposer = Disposer()

                            val globalIdx = areaNpcListCtrl.globalIndex(npc)
                            val idxSpan = span {
                                className = "pw-quest-editor-overlay-entity-index"
                                textContent = "#$globalIdx"
                            }

                            val secSpan = span {}
                            disposer.add(npc.sectionId.observeNow { secSpan.textContent = "Sec $it" })
                            secSpan.className = "pw-quest-editor-overlay-entity-detail"

                            val waveSpan = span {}
                            disposer.add(npc.wave.observeNow { waveSpan.textContent = "Wave ${it.id}" })
                            waveSpan.className = "pw-quest-editor-overlay-entity-detail"

                            val row = div {
                                className = "pw-quest-editor-overlay-entity-row"

                                onclick = { e ->
                                    e.stopPropagation()
                                    areaNpcListCtrl.selectNpc(npc)
                                }

                                appendChild(idxSpan)
                                span {
                                    className = "pw-quest-editor-overlay-entity-name"
                                    textContent = npc.type.simpleName
                                }
                                appendChild(secSpan)
                                appendChild(waveSpan)
                            }

                            disposer.add(areaNpcListCtrl.isSelected(npc).observeNow { selected ->
                                if (selected) {
                                    row.classList.add("pw-selected")
                                } else {
                                    row.classList.remove("pw-selected")
                                }
                            })

                            Pair(row, disposer)
                        }
                    }

                    observe(areaNpcListCtrl.currentAreaIdentifier) {
                        npcBody.scrollTop = 0.0
                    }
                }

                // Object panel
                div {
                    className = "pw-quest-editor-overlay-panel"

                    div {
                        className = "pw-quest-editor-overlay-panel-header"
                        span {
                            className = "pw-quest-editor-overlay-panel-title"
                            observeNow(areaObjectListCtrl.objects) { objects ->
                                textContent = "Objects (${objects.size})"
                            }
                        }
                    }

                    val objBody = div {
                        className = "pw-quest-editor-overlay-panel-body"

                        bindDisposableChildrenTo(areaObjectListCtrl.objects) { obj, _ ->
                            val disposer = Disposer()

                            val globalIdx = areaObjectListCtrl.globalIndex(obj)
                            val idxSpan = span {
                                className = "pw-quest-editor-overlay-entity-index"
                                textContent = "#$globalIdx"
                            }

                            val secSpan = span {}
                            disposer.add(obj.sectionId.observeNow { secSpan.textContent = "Sec $it" })
                            secSpan.className = "pw-quest-editor-overlay-entity-detail"

                            val row = div {
                                className = "pw-quest-editor-overlay-entity-row"

                                onclick = { e ->
                                    e.stopPropagation()
                                    areaObjectListCtrl.selectObject(obj)
                                }

                                appendChild(idxSpan)
                                span {
                                    className = "pw-quest-editor-overlay-entity-name"
                                    textContent = obj.type.simpleName
                                }
                                appendChild(secSpan)
                            }

                            disposer.add(areaObjectListCtrl.isSelected(obj).observeNow { selected ->
                                if (selected) {
                                    row.classList.add("pw-selected")
                                } else {
                                    row.classList.remove("pw-selected")
                                }
                            })

                            Pair(row, disposer)
                        }
                    }

                    observe(areaObjectListCtrl.currentAreaIdentifier) {
                        objBody.scrollTop = 0.0
                    }
                }
            }
        }

        element.appendChild(overlayContainer)

        // Hide overlay panels when no quest is loaded
        observeNow(areaNpcListCtrl.unavailable) { unavailable ->
            overlayContainer.style.display = if (unavailable) "none" else ""
        }
    }

    override fun dispose() {
        documentMouseDownListener?.dispose()
        documentKeyDownListener?.dispose()
        contextMenuPopup?.remove()
        super.dispose()
    }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-quest-editor-context-menu {
                    position: fixed;
                    min-width: 180px;
                }

                .pw-quest-editor-overlay-panels {
                    position: absolute;
                    top: 8px;
                    right: 0;
                    display: flex;
                    flex-direction: column;
                    gap: 4px;
                    z-index: 1000;
                }

                .pw-quest-editor-overlay-panel {
                    background: rgba(0, 0, 0, 0.8);
                    border-radius: 4px 0 0 4px;
                    min-width: 200px;
                    max-width: 260px;
                    border-left: 3px solid hsl(210, 70%, 55%);
                    transform: translateX(calc(100% - 28px));
                    transition: transform 0.25s ease-out, opacity 0.25s ease-out;
                    opacity: 0.7;
                }

                .pw-quest-editor-overlay-panel:hover {
                    transform: translateX(0);
                    opacity: 1;
                }

                .pw-quest-editor-overlay-panel-header {
                    display: flex;
                    align-items: center;
                    padding: 5px 10px;
                    user-select: none;
                    font-size: 12px;
                    font-weight: bold;
                    color: #eee;
                }

                .pw-quest-editor-overlay-panel-title {
                    flex: 1;
                    white-space: nowrap;
                }

                .pw-quest-editor-overlay-panel-body {
                    max-height: 0;
                    overflow: hidden;
                    transition: max-height 0.2s ease-out;
                }

                .pw-quest-editor-overlay-panel:hover .pw-quest-editor-overlay-panel-body {
                    max-height: 300px;
                    overflow-y: auto;
                    border-top: 1px solid rgba(255, 255, 255, 0.15);
                }

                .pw-quest-editor-overlay-entity-row {
                    display: flex;
                    align-items: center;
                    padding: 2px 10px;
                    cursor: pointer;
                    user-select: none;
                    font-size: 12px;
                    color: #ccc;
                }

                .pw-quest-editor-overlay-entity-row:hover {
                    background: rgba(255, 255, 255, 0.12);
                }

                .pw-quest-editor-overlay-entity-row.pw-selected {
                    background: hsla(210, 60%, 40%, 0.8);
                    color: #fff;
                }

                .pw-quest-editor-overlay-entity-row.pw-selected:hover {
                    background: hsla(210, 60%, 45%, 0.85);
                }

                .pw-quest-editor-overlay-entity-name {
                    flex: 1;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                }

                .pw-quest-editor-overlay-entity-index {
                    min-width: 28px;
                    color: rgba(255, 255, 255, 0.4);
                    font-size: 11px;
                    white-space: nowrap;
                }

                .pw-quest-editor-overlay-entity-row.pw-selected .pw-quest-editor-overlay-entity-index {
                    color: rgba(255, 255, 255, 0.6);
                }

                .pw-quest-editor-overlay-entity-detail {
                    margin-left: 6px;
                    color: rgba(255, 255, 255, 0.5);
                    white-space: nowrap;
                    font-size: 11px;
                }

                .pw-quest-editor-overlay-entity-row.pw-selected .pw-quest-editor-overlay-entity-detail {
                    color: rgba(255, 255, 255, 0.7);
                }
            """.trimIndent())
        }
    }
}
