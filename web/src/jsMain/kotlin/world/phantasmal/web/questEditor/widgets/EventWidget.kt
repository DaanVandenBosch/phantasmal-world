package world.phantasmal.web.questEditor.widgets

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.Node
import world.phantasmal.cell.map
import world.phantasmal.cell.mutateDeferred
import world.phantasmal.web.questEditor.controllers.EventsController
import world.phantasmal.web.questEditor.controllers.PlaybackState
import world.phantasmal.web.questEditor.models.QuestEventModel
import world.phantasmal.webui.dom.*
import world.phantasmal.webui.widgets.Dropdown
import world.phantasmal.webui.widgets.IntInput
import world.phantasmal.webui.widgets.Widget

class EventWidget(
    private val ctrl: EventsController,
    private val event: QuestEventModel,
) : Widget() {
    private val isSelected = ctrl.isSelected(event)
    private val isMultiSelected = ctrl.isMultiSelected(event)
    private val hasMultiSelection = ctrl.hasMultiSelection()
    private val npcsSummary = ctrl.getEventNpcsSummary(event)
    private val multiSelectedNpcsSummary = ctrl.getMultiSelectedEventNpcsSummary()
    private val isPlaybackCurrent = map(ctrl.playbackState, ctrl.playbackIndex, ctrl.events) { state, index, evts ->
        state != PlaybackState.Stopped && index in evts.indices && evts[index] === event
    }

    override fun Node.createElement() =
        div {
            className = "pw-quest-editor-event"
            toggleClass("pw-selected", isSelected)
            toggleClass("pw-multi-selected", isMultiSelected)
            toggleClass("pw-playback-current", isPlaybackCurrent)
            tabIndex = 0

            onclick = { e ->
                e.stopPropagation()
                ctrl.onEventClickedDuringPlayback(event)
                val isMultiSelectKey = e.ctrlKey || e.metaKey

                if (isMultiSelectKey) {
                    ctrl.selectEvent(event, true)
                } else {
                    ctrl.selectEvent(event, false)
                    mutateDeferred { ctrl.goToEventSection(event) }
                }
            }

            onkeyup = { e ->
                if ((e.target as? Element)?.nodeName != "INPUT") {
                    when (e.key) {
                        "Enter" -> ctrl.selectEvent(event)
                        "Delete" -> ctrl.removeEvent(event)
                    }
                }
            }

            createPropsSection()
            createActionsSection()
        }

    private fun Node.createPropsSection() {
        div {
            className = "pw-quest-editor-event-props"

            val overlay = (document.createElement("div") as HTMLDivElement).apply {
                className = "pw-quest-editor-event-monster-overlay"
            }
            document.body?.appendChild(overlay)

            var hoverTimerId: Int? = null
            var isHovering = false

            addDisposable(world.phantasmal.core.disposable.disposable {
                hoverTimerId?.let { window.clearTimeout(it) }
                overlay.remove()
            })

            fun showOverlay(anchorElement: Element) {
                if (!isHovering) return

                val hasMulti = hasMultiSelection.value
                val inMulti = isMultiSelected.value
                val currentSummary = if (hasMulti && inMulti) {
                    multiSelectedNpcsSummary.value ?: npcsSummary.value ?: NO_NPCS_LABEL
                } else {
                    npcsSummary.value ?: NO_NPCS_LABEL
                }

                overlay.textContent = currentSummary
                val rect = anchorElement.getBoundingClientRect()
                overlay.style.left = "${rect.right + 2}px"
                overlay.style.top = "${rect.top}px"
                overlay.style.display = "block"

                val overlayHeight = overlay.offsetHeight
                val viewportHeight = window.innerHeight
                val spaceBelow = viewportHeight - rect.top

                if (overlayHeight > spaceBelow) {
                    overlay.style.top = "${rect.bottom - overlayHeight}px"
                }
            }

            fun hideOverlay() {
                isHovering = false
                hoverTimerId?.let { window.clearTimeout(it) }
                hoverTimerId = null
                overlay.style.display = "none"
            }

            table {
                createIdRow(::showOverlay, ::hideOverlay) { isHovering = true; hoverTimerId = it }
                createInputRow("Section:", event.sectionId, min = 0) { ctrl.setSectionId(event, it) }
                createInputRow("Wave:", event.wave.map { it.id }, min = 1) { ctrl.setWaveId(event, it) }
                createInputRow("Delay:", event.delay, min = 0) { ctrl.setDelay(event, it) }
                createChallengeSettings()
            }
        }
    }

    private fun Node.createIdRow(
        showOverlay: (Element) -> Unit,
        hideOverlay: () -> Unit,
        setHoverState: (Int) -> Unit,
    ) {
        tr {
            val idInput = IntInput(
                enabled = ctrl.enabled,
                value = event.id,
                onChange = { ctrl.setId(event, it) },
                label = "ID:",
                min = 0,
                step = 1,
            )
            th {
                val labelElement = idInput.label!!
                addChild(labelElement)

                labelElement.element.onmouseenter = { _ ->
                    val timerId = window.setTimeout({ showOverlay(labelElement.element) }, HOVER_DELAY_MS)
                    setHoverState(timerId)
                }
                labelElement.element.onmouseleave = { hideOverlay() }
            }
            td { addChild(idInput) }
        }
    }

    private fun Node.createInputRow(
        label: String,
        value: world.phantasmal.cell.Cell<Int>,
        min: Int,
        onChange: (Int) -> Unit,
    ) {
        tr {
            val input = IntInput(
                enabled = ctrl.enabled,
                value = value,
                onChange = onChange,
                label = label,
                min = min,
                step = 1,
            )
            th { addChild(input.label!!) }
            td { addChild(input) }
        }
    }

    private fun Node.createChallengeSettings() {
        // Container tbody for challenge mode rows, rebuilt when cmWaveSettings changes.
        val container = tbody {}

        observeNow(event.cmWaveSettings) { settings ->
            // Clear previous rows to avoid DOM leaks on repeated changes.
            while (container.firstChild != null) {
                container.removeChild(container.firstChild!!)
            }

            if (settings != null) {
                container.createInputRow("CM Min:", event.cmMinEnemies, min = 0) { ctrl.setCmMinEnemies(event, it) }
                container.createInputRow("CM Max:", event.cmMaxEnemies, min = 0) { ctrl.setCmMaxEnemies(event, it) }
                container.createInputRow("CM Max Waves:", event.cmMaxWaves, min = 0) { ctrl.setCmMaxWaves(event, it) }
            }
        }
    }

    private fun Node.createActionsSection() {
        div {
            className = "pw-quest-editor-event-actions"

            table {
                thead {
                    tr {
                        th {
                            colSpan = 3
                            textContent = "Actions:"
                        }
                    }
                }
                tbody {
                    bindChildWidgetsTo(event.actions) { action, _ ->
                        EventActionWidget(ctrl, event, action)
                    }
                }
                tfoot {
                    tr {
                        td {
                            colSpan = 3
                            addWidget(Dropdown(
                                enabled = ctrl.enabled,
                                text = "Add action",
                                items = ctrl.eventActionTypes,
                                onSelect = { ctrl.addAction(event, it) }
                            ))
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val HOVER_DELAY_MS = 300
        private const val NO_NPCS_LABEL = "(No NPCs)"

        init {
            @Suppress("CssUnusedSymbol", "CssUnresolvedCustomProperty")
            // language=css
            style(
                """
                .pw-quest-editor-event {
                    display: flex;
                    flex-wrap: wrap;
                    border: var(--pw-border);
                    margin: 2px;
                    background-color: hsl(0, 0%, 17%);
                    outline: none;
                }

                .pw-quest-editor-event:hover, .pw-quest-editor-event:focus {
                    border-color: hsl(0, 0%, 30%);
                    background-color: hsl(0, 0%, 20%);
                    color: hsl(0, 0%, 85%);
                }

                .pw-quest-editor-event.pw-selected {
                    border-color: hsl(0, 0%, 35%);
                    background-color: hsl(0, 0%, 25%);
                    color: hsl(0, 0%, 90%);
                }

                .pw-quest-editor-event.pw-multi-selected {
                    border-color: hsl(200, 100%, 50%);
                    background-color: hsl(200, 50%, 20%);
                    color: hsl(0, 0%, 90%);
                    box-shadow: 0 0 3px hsl(200, 100%, 50%);
                }

                .pw-quest-editor-event.pw-selected.pw-multi-selected {
                    border-color: hsl(200, 100%, 60%);
                    background-color: hsl(200, 50%, 25%);
                    box-shadow: 0 0 5px hsl(200, 100%, 60%);
                }

                .pw-quest-editor-event.pw-playback-current {
                    border-color: hsl(130, 70%, 45%);
                    background-color: hsl(130, 30%, 20%);
                    box-shadow: 0 0 6px hsl(130, 70%, 45%);
                }

                .pw-quest-editor-event-props, .pw-quest-editor-event-actions {
                    padding: 2px 6px;
                }

                .pw-quest-editor-event-props {
                    width: 150px;
                }

                .pw-quest-editor-event-actions {
                    width: 165px;
                }
                
                .pw-quest-editor-event > div > table {
                    width: 100%;
                    border-collapse: collapse;
                }

                .pw-quest-editor-event th {
                    text-align: left;
                    white-space: nowrap;
                }

                /* Monster overlay (fixed position, appended to body) */
                .pw-quest-editor-event-monster-overlay {
                    display: none;
                    position: fixed;
                    background-color: hsl(0, 0%, 10%);
                    color: hsl(0, 0%, 90%);
                    padding: 8px 12px;
                    border-radius: 4px;
                    border: 1px solid hsl(0, 0%, 30%);
                    font-size: 12px;
                    font-family: monospace;
                    white-space: pre;
                    z-index: 10000;
                    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.5);
                    pointer-events: none;
                }
                """.trimIndent()
            )
        }
    }
}
