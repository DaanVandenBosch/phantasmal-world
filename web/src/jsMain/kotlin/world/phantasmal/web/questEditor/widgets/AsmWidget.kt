package world.phantasmal.web.questEditor.widgets

import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.Node
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.psolib.asm.OPCODES
import world.phantasmal.psolib.asm.OPCODES_F8
import world.phantasmal.psolib.asm.OPCODES_F9
import world.phantasmal.psolib.asm.Opcode
import world.phantasmal.cell.map
import world.phantasmal.web.questEditor.controllers.AsmEditorController
import world.phantasmal.web.shared.messages.SegmentInfoType
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.dom.input
import world.phantasmal.webui.dom.span
import world.phantasmal.webui.widgets.Widget

class AsmWidget(private val ctrl: AsmEditorController) : Widget() {
    private lateinit var editorWidget: AsmEditorWidget

    // Opcode navigation state: track which opcode was last clicked and which occurrence index
    private var lastClickedMnemonic: String? = null
    private var lastClickedIndex: Int = -1

    // Register navigation state
    private var lastClickedRegId: Int? = null
    private var lastClickedRegIndex: Int = -1

    override fun Node.createElement() =
        div {
            className = "pw-quest-editor-asm"

            addChild(AsmToolbarWidget(ctrl))

            // Editor area with overlay panels
            div {
                className = "pw-asm-editor-area"

                editorWidget = addChild(AsmEditorWidget(ctrl))

                // Overlay panels (slide-in from right)
                div {
                    className = "pw-asm-overlay-panels"

                    createLabelListPanel(this)
                    createRegisterListPanel(this)
                    createSegmentListPanel(this)
                    createOpcodeReferencePanel(this)
                }
            }
        }

    private fun createLabelListPanel(parent: HTMLDivElement) {
        parent.apply {
            div {
                className = "pw-asm-overlay-panel"

                div {
                    className = "pw-asm-overlay-panel-header"
                    span {
                        className = "pw-asm-overlay-panel-title"
                        observeNow(ctrl.labels) { labels ->
                            textContent = "Labels (${labels.size})"
                        }
                    }
                }

                val body = div {
                    className = "pw-asm-overlay-panel-body"

                    bindDisposableChildrenTo(ctrl.labels.map { it.sortedBy { l -> l.name } }) { label, _ ->
                        val row = div {
                            className = "pw-asm-overlay-row pw-asm-overlay-row-clickable"

                            onclick = {
                                ctrl.goToLabelRange(label.range)
                            }

                            span {
                                className = "pw-asm-overlay-row-name"
                                textContent = "${label.name}"
                            }
                            span {
                                className = "pw-asm-overlay-row-detail"
                                textContent = "L${label.range.startLineNo}"
                            }
                        }

                        Pair(row, Disposer())
                    }
                }

                observeNow(ctrl.labels) {
                    body.scrollTop = 0.0
                }
            }
        }
    }

    private fun createRegisterListPanel(parent: HTMLDivElement) {
        parent.apply {
            div {
                className = "pw-asm-overlay-panel"

                div {
                    className = "pw-asm-overlay-panel-header"
                    span {
                        className = "pw-asm-overlay-panel-title"
                        observeNow(ctrl.registers) { regs ->
                            textContent = "Registers (${regs.size})"
                        }
                    }
                }

                div {
                    className = "pw-asm-overlay-panel-body"

                    bindDisposableChildrenTo(ctrl.registers) { reg, _ ->
                        val rwText = buildString {
                            if (reg.reads > 0) append("R${reg.reads}")
                            if (reg.reads > 0 && reg.writes > 0) append("/")
                            if (reg.writes > 0) append("W${reg.writes}")
                        }

                        val row = div {
                            className = "pw-asm-overlay-row pw-asm-overlay-row-clickable"

                            onclick = {
                                navigateToRegister(reg.id)
                            }

                            span {
                                className = "pw-asm-overlay-row-name"
                                textContent = "r${reg.id}"
                            }
                            span {
                                className = "pw-asm-overlay-row-detail"
                                textContent = rwText
                            }
                        }

                        Pair(row, Disposer())
                    }
                }
            }
        }
    }

    private fun createSegmentListPanel(parent: HTMLDivElement) {
        parent.apply {
            div {
                className = "pw-asm-overlay-panel"

                div {
                    className = "pw-asm-overlay-panel-header"
                    span {
                        className = "pw-asm-overlay-panel-title"
                        observeNow(ctrl.segments) { segs ->
                            textContent = "Data/String (${segs.size})"
                        }
                    }
                }

                div {
                    className = "pw-asm-overlay-panel-body"

                    bindDisposableChildrenTo(ctrl.segments) { seg, _ ->
                        val typeStr = when (seg.type) {
                            SegmentInfoType.Data -> ".data"
                            SegmentInfoType.String -> ".string"
                        }
                        val labelStr = seg.labels.joinToString(", ")
                        val sizeStr = when (seg.type) {
                            SegmentInfoType.Data -> "${seg.size}B"
                            SegmentInfoType.String -> "${seg.size}ch"
                        }

                        val row = div {
                            className = "pw-asm-overlay-row pw-asm-overlay-row-clickable"

                            onclick = {
                                ctrl.goToLabelRange(seg.range)
                            }

                            span {
                                className = "pw-asm-overlay-row-name"
                                textContent = "$typeStr $labelStr"
                            }
                            span {
                                className = "pw-asm-overlay-row-detail"
                                textContent = sizeStr
                            }
                        }

                        Pair(row, Disposer())
                    }
                }
            }
        }
    }

    private fun createOpcodeReferencePanel(parent: HTMLDivElement) {
        parent.apply {
            div {
                className = "pw-asm-overlay-panel pw-asm-opcode-panel"

                div {
                    className = "pw-asm-overlay-panel-header"
                    span {
                        className = "pw-asm-overlay-panel-title"
                        textContent = "Opcodes"
                    }
                }

                div {
                    className = "pw-asm-overlay-panel-body"

                    val filterInput = input {
                        className = "pw-asm-opcode-filter"
                        type = "text"
                        placeholder = "Filter..."

                        // Stop click/keyboard events from propagating to Monaco editor
                        onclick = { it.stopPropagation() }
                        onkeydown = { it.stopPropagation() }
                        onkeyup = { it.stopPropagation() }
                    }

                    val listContainer = div {
                        className = "pw-asm-opcode-list"
                    }

                    // Build all opcode entries
                    val allOpcodes = buildAllOpcodes()
                    renderOpcodeList(listContainer, allOpcodes)

                    filterInput.oninput = {
                        val filter = (it.target as HTMLInputElement).value.lowercase()
                        renderOpcodeList(listContainer, if (filter.isEmpty()) {
                            allOpcodes
                        } else {
                            allOpcodes.filter { op ->
                                op.mnemonic.contains(filter) ||
                                    op.code.toString(16).contains(filter) ||
                                    (op.doc?.lowercase()?.contains(filter) == true)
                            }
                        })
                    }
                }
            }
        }
    }

    private fun navigateToRegister(regId: Int) {
        val matches = ctrl.findRegisterMatches(regId)

        if (matches.isEmpty()) return

        // Advance to next occurrence, or start from 0 if different register
        lastClickedRegIndex = if (regId == lastClickedRegId) {
            (lastClickedRegIndex + 1) % matches.size
        } else {
            0
        }
        lastClickedRegId = regId

        ctrl.goToMatch(matches[lastClickedRegIndex])
    }

    private fun navigateToOpcode(mnemonic: String) {
        val matches = ctrl.findOpcodeMatches(mnemonic)

        if (matches.isEmpty()) return

        // Advance to next occurrence, or start from 0 if different opcode
        lastClickedIndex = if (mnemonic == lastClickedMnemonic) {
            (lastClickedIndex + 1) % matches.size
        } else {
            0
        }
        lastClickedMnemonic = mnemonic

        ctrl.goToMatch(matches[lastClickedIndex])
    }

    private fun buildAllOpcodes(): List<Opcode> {
        val result = mutableListOf<Opcode>()

        for (op in OPCODES) {
            if (op != null && op.known) result.add(op)
        }
        for (op in OPCODES_F8) {
            if (op != null && op.known) result.add(op)
        }
        for (op in OPCODES_F9) {
            if (op != null && op.known) result.add(op)
        }

        return result
    }

    private fun renderOpcodeList(container: HTMLDivElement, opcodes: List<Opcode>) {
        container.innerHTML = ""

        for (opcode in opcodes) {
            val codeStr = if (opcode.code <= 0xFF) {
                "0x${opcode.code.toString(16).uppercase().padStart(2, '0')}"
            } else {
                "0x${opcode.code.toString(16).uppercase().padStart(4, '0')}"
            }

            val doc = container.ownerDocument!!
            val row = doc.createElement("div") as HTMLDivElement
            row.className = "pw-asm-overlay-row pw-asm-overlay-row-clickable"
            row.title = opcode.doc ?: ""

            row.onclick = {
                navigateToOpcode(opcode.mnemonic)
            }

            val nameSpan = doc.createElement("span") as org.w3c.dom.HTMLSpanElement
            nameSpan.className = "pw-asm-overlay-row-name"
            nameSpan.textContent = opcode.mnemonic
            row.appendChild(nameSpan)

            val codeSpan = doc.createElement("span") as org.w3c.dom.HTMLSpanElement
            codeSpan.className = "pw-asm-overlay-row-detail"
            codeSpan.textContent = codeStr
            row.appendChild(codeSpan)

            container.appendChild(row)
        }
    }

    override fun focus() {
        editorWidget.focus()
    }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-quest-editor-asm {
                    display: flex;
                    flex-direction: column;
                    overflow: hidden;
                }

                .pw-asm-editor-area {
                    position: relative;
                    flex-grow: 1;
                    display: flex;
                    overflow: hidden;
                }

                .pw-asm-overlay-panels {
                    position: absolute;
                    top: 4px;
                    right: 14px;
                    display: flex;
                    flex-direction: column;
                    gap: 4px;
                    z-index: 20;
                    pointer-events: none;
                    overflow-x: clip;
                }

                .pw-asm-overlay-panel {
                    pointer-events: auto;
                    position: relative;
                    background: rgba(0, 0, 0, 0.85);
                    border-radius: 4px 0 0 4px;
                    min-width: 160px;
                    max-width: 220px;
                    border-left: 3px solid hsl(210, 70%, 55%);
                    transform: translateX(calc(100% - 28px));
                    transition: transform 0.25s ease-out, opacity 0.25s ease-out;
                    opacity: 0.6;
                }

                .pw-asm-overlay-panel:hover {
                    transform: translateX(0);
                    opacity: 1;
                }

                .pw-asm-overlay-panel-header {
                    display: flex;
                    align-items: center;
                    padding: 4px 10px;
                    user-select: none;
                    font-size: 12px;
                    font-weight: bold;
                    color: #eee;
                }

                .pw-asm-overlay-panel-title {
                    flex: 1;
                    white-space: nowrap;
                }

                .pw-asm-overlay-panel-body {
                    position: absolute;
                    top: 100%;
                    left: -3px;
                    right: 0;
                    max-height: 0;
                    overflow: hidden;
                    transition: max-height 0.2s ease-out;
                    background: rgba(0, 0, 0, 0.85);
                    border-left: 3px solid hsl(210, 70%, 55%);
                    border-radius: 0 0 0 4px;
                    z-index: 1;
                }

                .pw-asm-overlay-panel:hover .pw-asm-overlay-panel-body {
                    max-height: 300px;
                    overflow-y: auto;
                    border-top: 1px solid rgba(255, 255, 255, 0.15);
                }

                .pw-asm-overlay-row {
                    display: flex;
                    align-items: center;
                    padding: 2px 10px;
                    user-select: none;
                    font-size: 12px;
                    color: #ccc;
                }

                .pw-asm-overlay-row-clickable {
                    cursor: pointer;
                }

                .pw-asm-overlay-row-clickable:hover {
                    background: rgba(255, 255, 255, 0.12);
                }

                .pw-asm-overlay-row-name {
                    flex: 1;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                }

                .pw-asm-overlay-row-detail {
                    margin-left: 6px;
                    color: rgba(255, 255, 255, 0.5);
                    white-space: nowrap;
                    font-size: 11px;
                }

                .pw-asm-opcode-panel:hover .pw-asm-overlay-panel-body {
                    max-height: 400px;
                }

                .pw-asm-opcode-filter {
                    display: block;
                    width: calc(100% - 16px);
                    margin: 4px 8px;
                    padding: 3px 6px;
                    background: rgba(255, 255, 255, 0.1);
                    border: 1px solid rgba(255, 255, 255, 0.2);
                    border-radius: 3px;
                    color: #eee;
                    font-size: 12px;
                    outline: none;
                }

                .pw-asm-opcode-filter:focus {
                    border-color: hsl(210, 70%, 55%);
                }

                .pw-asm-opcode-filter::placeholder {
                    color: rgba(255, 255, 255, 0.4);
                }

                .pw-asm-opcode-list {
                    max-height: 350px;
                    overflow-y: auto;
                }
            """.trimIndent())
        }
    }
}
