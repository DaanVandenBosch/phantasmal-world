package world.phantasmal.web.questEditor.widgets

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import world.phantasmal.cell.Cell
import world.phantasmal.cell.cell
import world.phantasmal.cell.mutableCell
import world.phantasmal.cell.observeNow
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.psolib.fileFormats.quest.ObjectType
import world.phantasmal.psolib.symbolchat.SymbolChatColliTable
import world.phantasmal.web.questEditor.commands.EditEntityPropCommand
import world.phantasmal.web.questEditor.loading.SymbolChatColliRepository
import world.phantasmal.web.questEditor.models.QuestEntityPropModel
import world.phantasmal.web.questEditor.models.QuestObjectModel
import world.phantasmal.web.questEditor.stores.QuestEditorStore
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.dom.span
import world.phantasmal.webui.widgets.Button
import world.phantasmal.webui.widgets.Checkbox
import world.phantasmal.webui.widgets.Dialog
import world.phantasmal.webui.widgets.IntInput
import world.phantasmal.webui.widgets.Widget

/**
 * Popup invoked from the 3D viewport's right-click menu when a
 * `SymbolChatObject` is selected. Edits the three switch-gated stages of
 * the object's `spec1`/`spec2`/`spec3` fields (each a `(SC Flag, SC ID)`
 * pair) through a single visual surface.
 *
 * Runtime game semantics this UI mirrors (see `docs/symbol-chat-object.md`):
 * - Player walks into the object's radius.
 * - Game evaluates spec3 → spec2 → spec1 in reverse; the first spec whose
 *   switch flag is NOT set determines which SC is shown. An out-of-range
 *   SC ID (e.g. `sc30`) means "show nothing at this stage".
 *
 * All edits route through `EditEntityPropCommand` so they're undoable.
 */
class SymbolChatEditPopup(
    visible: Cell<Boolean>,
    private val questEditorStore: QuestEditorStore,
    private val symbolChatColliRepository: SymbolChatColliRepository,
    onDismiss: () -> Unit,
) : Dialog(
    visible = visible,
    title = cell("Edit Symbol Chat"),
    description = cell("Each stage advances when its switch flag is set. An out-of-range SC ID means \"show nothing\" at that stage."),
    content = {},
    footer = {},
    onDismiss = onDismiss,
) {
    private val stageDisposer = addDisposable(Disposer())

    /**
     * The entity the popup was opened for — captured on visible→true and
     * cleared on visible→false. If the user changes selection while the
     * popup is open, we compare against this reference: any mismatch
     * (including switching to a different SymbolChatObject) closes the
     * popup so edits can't land on the wrong entity.
     */
    private var openedForEntity: QuestObjectModel? = null

    /**
     * Must be declared before the `init` block: `buildBody()` is called from
     * init and writes into this list. Kotlin initializes properties in
     * declaration order interleaved with init blocks, so a property below
     * the init block would still be `undefined` (JS) / `null` (JVM) at the
     * time of access and crash with a "Cannot read properties of undefined"
     * TypeError.
     */
    private val stageContainers = mutableListOf<HTMLElement>()

    init {
        val bodyElement = dialogElement.querySelector(".pw-dialog-body") as? HTMLElement
        bodyElement?.let {
            it.innerHTML = ""
            it.style.setProperty("overflow-y", "auto")
            it.appendChild(buildBody())
        }

        val footerElement = dialogElement.querySelector(".pw-dialog-footer") as? HTMLElement
        footerElement?.let { footer ->
            footer.innerHTML = ""
            val closeBtn = addDisposable(Button(text = "Close", onClick = { onDismiss() }))
            footer.appendChild(closeBtn.element)
        }

        // Close if the selection no longer matches the entity we opened for.
        // Covers both "switched to non-SC entity" and "switched to a different
        // SymbolChatObject" — neither should silently retarget the editor.
        observeNow(questEditorStore.selectedEntity) { entity ->
            val target = openedForEntity
            if (visible.value && target != null && entity !== target) {
                onDismiss()
            }
        }

        // (Re)build stage sections when visible→true; capture the target
        // entity so subsequent selection changes can detect drift.
        observeNow(visible) { vis ->
            stageDisposer.disposeAll()
            if (vis) {
                val entity = questEditorStore.selectedEntity.value as? QuestObjectModel
                if (entity == null || entity.type != ObjectType.SymbolChatObject) {
                    // Nothing sensible to edit — close and bail before we
                    // touch the DOM with stale refs.
                    openedForEntity = null
                    onDismiss()
                    return@observeNow
                }
                openedForEntity = entity
                rebuildStagesFor(entity)
                centerDialog()
            } else {
                openedForEntity = null
            }
        }
    }

    private fun centerDialog() {
        dialogElement.style.width = "${DIALOG_WIDTH}px"
        dialogElement.style.maxHeight = "${DIALOG_MAX_HEIGHT}px"
        val x = (window.innerWidth - DIALOG_WIDTH) / 2
        val y = maxOf(20, (window.innerHeight - DIALOG_MAX_HEIGHT) / 2)
        dialogElement.style.transform = "translate(${x}px, ${y}px)"
    }

    private fun buildBody(): HTMLElement {
        val root = document.createElement("div") as HTMLElement
        root.className = "pw-sc-edit-popup-body"

        for (slot in 1..3) {
            val container = document.createElement("div") as HTMLElement
            container.className = "pw-sc-edit-stage"
            stageContainers.add(container)
            root.appendChild(container)
        }
        return root
    }

    private fun rebuildStagesFor(entity: QuestObjectModel) {
        for (slot in 1..3) {
            val container = stageContainers.getOrNull(slot - 1) ?: continue
            container.innerHTML = ""
            renderStage(container, entity, slot)
        }
    }

    private fun renderStage(container: HTMLElement, entity: QuestObjectModel, slot: Int) {
        val idProp = entity.properties.value.find { it.name == "SC ID $slot" } ?: return
        val flagProp = entity.properties.value.find { it.name == "SC Flag $slot" } ?: return

        // Header describing which switch state shows this stage.
        val header = document.createElement("div") as HTMLElement
        header.className = "pw-sc-edit-stage-header"
        header.textContent = stageHeaderText(slot)
        container.appendChild(header)

        // Row: switch flag IntInput + hide checkbox.
        val row = document.createElement("div") as HTMLElement
        row.className = "pw-sc-edit-stage-row"

        val flagCell = mutableCell(flagProp.value.value as? Int ?: 0)
        stageDisposer.add(flagProp.value.observeNow { new ->
            val int = new as? Int ?: return@observeNow
            if (flagCell.value != int) flagCell.value = int
        })
        // IntInput's `label` parameter renders a real `<label for="...">`
        // element wired to the input's id for a11y. The flag's effect
        // differs per stage (stage 1/2 advance; stage 3 disappears), but the
        // stage header above already describes the resulting state — a
        // stage-specific name in the field label would be wordier without
        // adding information, so a plain "Switch flag:" works.
        val flagInput = IntInput(
            label = "Switch flag:",
            value = flagCell,
            onChange = { v -> applyEdit(entity, flagProp, v) },
            min = 0,
            max = 0xFFFF,
        )
        stageDisposer.add(flagInput)
        flagInput.label?.let { labelWidget ->
            stageDisposer.add(labelWidget)
            labelWidget.element.classList.add("pw-sc-edit-stage-label")
            row.appendChild(labelWidget.element)
        }
        row.appendChild(flagInput.element)

        // Hide checkbox: toggles SC ID between an in-range default and the
        // sc30 sentinel. Driven by whether the current id is out of range.
        val currentId = idProp.value.value as? Int
        val hideCell = mutableCell(isSentinel(currentId))
        // Last seen in-range id, used to restore a reasonable value when the
        // user flips hide back off. Initialised to the starting value if it
        // was in range, else 0 as a safe default.
        var lastInRangeId: Int =
            if (currentId != null && !isSentinel(currentId)) currentId else 0
        stageDisposer.add(idProp.value.observeNow { new ->
            val int = new as? Int ?: return@observeNow
            if (!isSentinel(int)) lastInRangeId = int
            val hidden = isSentinel(int)
            if (hideCell.value != hidden) hideCell.value = hidden
        })
        val hideCheckbox = Checkbox(
            label = "hide (sc${SymbolChatColliTable.HIDE_SENTINEL_ID})",
            checked = hideCell,
            onChange = { checked ->
                val newId = if (checked) SymbolChatColliTable.HIDE_SENTINEL_ID else lastInRangeId
                applyEdit(entity, idProp, newId)
            },
        )
        stageDisposer.add(hideCheckbox)
        row.appendChild(hideCheckbox.element)

        container.appendChild(row)

        // 24-preset grid.
        val grid = document.createElement("div") as HTMLElement
        grid.className = "pw-sc-edit-stage-grid"

        val cellElements = mutableListOf<HTMLElement>()
        for (id in 0 until SymbolChatColliTable.ENTRY_COUNT) {
            val cellEl = document.createElement("div") as HTMLElement
            cellEl.className = "pw-sc-edit-stage-cell"
            cellEl.title = "SC $id"

            val cnv = document.createElement("canvas") as HTMLCanvasElement
            cnv.className = "pw-sc-edit-stage-canvas"
            cnv.width = SymbolChatRenderer.CANVAS_WIDTH
            cnv.height = SymbolChatRenderer.CANVAS_HEIGHT
            cellEl.appendChild(cnv)

            // Paint this cell's thumbnail. ensureLoaded fires synchronously
            // when the atlas is cached and defers otherwise — either way the
            // canvas is bound to the right id by closure, so we don't rely
            // on DOM insertion order to reverse-map index → SC ID later.
            val buf = symbolChatColliRepository.entry(id)
            if (buf != null) {
                SymbolChatRenderer.ensureLoaded {
                    SymbolChatRenderer.renderBuffer(cnv, buf)
                }
            }

            val labelEl = document.createElement("span") as HTMLElement
            labelEl.className = "pw-sc-edit-stage-cell-label"
            labelEl.textContent = "#$id"
            cellEl.appendChild(labelEl)

            cellEl.addEventListener("click", {
                applyEdit(entity, idProp, id)
            })

            grid.appendChild(cellEl)
            cellElements.add(cellEl)
        }

        // Highlight the currently-selected preset (or none if sentinel).
        stageDisposer.add(idProp.value.observeNow { new ->
            val currentId = new as? Int
            for ((i, el) in cellElements.withIndex()) {
                if (i == currentId) el.classList.add(SELECTED_CLASS)
                else el.classList.remove(SELECTED_CLASS)
            }
        })

        container.appendChild(grid)
    }

    private fun stageHeaderText(slot: Int): String = when (slot) {
        1 -> "Shown by default (before any switch flips)"
        2 -> "Shown after stage 1's switch flips"
        3 -> "Shown after stage 2's switch flips"
        else -> "Stage $slot"
    }

    private fun applyEdit(
        entity: QuestObjectModel,
        prop: QuestEntityPropModel,
        newValue: Int,
    ) {
        val oldValue = prop.value.value
        if (oldValue == newValue) return
        questEditorStore.executeAction(
            EditEntityPropCommand(
                questEditorStore,
                entity,
                prop,
                newValue = newValue,
                oldValue = oldValue,
            )
        )
    }

    private fun isSentinel(id: Int?): Boolean =
        id == null || id !in 0 until SymbolChatColliTable.ENTRY_COUNT

    companion object {
        private const val DIALOG_WIDTH = 540
        private const val DIALOG_MAX_HEIGHT = 640
        private const val SELECTED_CLASS = "pw-sc-edit-stage-cell-selected"

        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-sc-edit-popup-body {
                    display: flex;
                    flex-direction: column;
                    gap: 12px;
                }
                .pw-sc-edit-stage {
                    border: 1px solid #444;
                    border-radius: 4px;
                    padding: 8px;
                    background: rgba(255,255,255,0.02);
                }
                .pw-sc-edit-stage-header {
                    font-size: 13px;
                    font-weight: 600;
                    color: #eee;
                    margin-bottom: 6px;
                }
                .pw-sc-edit-stage-row {
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    margin-bottom: 6px;
                }
                .pw-sc-edit-stage-label {
                    font-size: 12px;
                    color: #ccc;
                }
                .pw-sc-edit-stage-grid {
                    display: grid;
                    grid-template-columns: repeat(6, 1fr);
                    gap: 4px;
                }
                .pw-sc-edit-stage-cell {
                    position: relative;
                    cursor: pointer;
                    border: 1px solid #444;
                    background: #181818;
                    padding: 2px;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                }
                .pw-sc-edit-stage-cell:hover {
                    border-color: #ffaa00;
                }
                .pw-sc-edit-stage-cell-selected {
                    border-color: #ffff00;
                    box-shadow: 0 0 0 1px #ffff00 inset;
                }
                .pw-sc-edit-stage-canvas {
                    image-rendering: pixelated;
                    width: 72px;
                    height: 40px;
                    background: #fff;
                }
                .pw-sc-edit-stage-cell-label {
                    font-size: 10px;
                    color: #aaa;
                    margin-top: 2px;
                }
            """.trimIndent())
        }
    }
}
