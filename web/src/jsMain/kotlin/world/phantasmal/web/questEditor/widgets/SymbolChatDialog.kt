package world.phantasmal.web.questEditor.widgets

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.Node
import org.w3c.dom.events.Event
import world.phantasmal.cell.Cell
import world.phantasmal.cell.MutableCell
import world.phantasmal.core.disposable.disposable
import world.phantasmal.cell.cell
import world.phantasmal.cell.map
import world.phantasmal.cell.mutableCell
import world.phantasmal.cell.mutateDeferred
import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.web.questEditor.asm.DataLabelType
import world.phantasmal.web.questEditor.controllers.DataEditorController
import world.phantasmal.web.questEditor.controllers.DataLabelEntry
import world.phantasmal.webui.dom.canvas
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.dom.span
import world.phantasmal.webui.dom.table
import world.phantasmal.webui.dom.tbody
import world.phantasmal.webui.dom.td
import world.phantasmal.webui.dom.th
import world.phantasmal.webui.dom.tr
import world.phantasmal.webui.widgets.Button
import world.phantasmal.webui.widgets.Checkbox
import world.phantasmal.webui.widgets.Dialog
import world.phantasmal.webui.widgets.IntInput
import world.phantasmal.webui.widgets.Select
import world.phantasmal.webui.widgets.Widget

/**
 * Edits a labelled symbol-chat data segment.
 *
 * Supports two layouts depending on [labelType]:
 *
 * **SymbolChatData** — 60-byte standard SymbolChatT (little-endian):
 *   face:    u32       — bits 0..1 = face index, upper bits = bg color
 *   corners: 4 × { icon: u8; param: u8 }
 *   parts:   12 × { partId: u8; posX: u8; posY: u8; mirror: u8 }
 *
 * **SymbolChatHexData** — 64-byte extended layout used by
 * `set_symbol_chat_collision` (auto-detected from opcode 0xF8A6):
 *   header:  4 × u8 — face type, face color, sound effect, reserved
 *   body:    60-byte standard SymbolChatT (same as above)
 *
 * Symbol chat labels have no opcode-based detection, so the user marks
 * them via "Mark label as: Symbol Chat" first. This dialog provides a
 * structured field editor plus a canvas preview rendered by
 * [SymbolChatRenderer]. Edits flow through the IntInput / picker fields;
 * the preview re-renders whenever any cell changes.
 *
 * GC endianness: detected on load via a simple heuristic (the `face` high
 * two bytes are zero in BE byte order). When true, the `face` dword is
 * read as 4-byte big-endian and each `corners` u16 (icon/param pair)
 * has its bytes swapped. `parts` are byte arrays and never swapped.
 * The detection is fragile (a small LE face dword looks the same as a
 * BE one), so a "GC big-endian" checkbox lets the user override.
 */
class SymbolChatDialog(
    visible: Cell<Boolean>,
    private val ctrl: DataEditorController,
    onDismiss: () -> Unit,
    private val initialLabelId: Cell<Int?> = cell(null),
    private val labelType: DataLabelType = DataLabelType.SymbolChatData,
) : Dialog(
    visible = visible,
    title = cell(if (labelType == DataLabelType.SymbolChatHexData) "Edit Symbol Chat (HEX)" else "Edit Symbol Chat"),
    description = cell(
        if (labelType == DataLabelType.SymbolChatHexData)
            "64-byte HEX symbol chat record (4-byte header + 60-byte SymbolChatT)."
        else
            "60-byte symbol chat record (little-endian)."
    ),
    content = {},
    footer = {},
    onDismiss = onDismiss,
) {
    companion object {
        const val SC_SIZE = 60
        const val HEX_SIZE = 64
        const val HEX_HEADER = 4
        const val CORNERS = 4
        const val PARTS = 12

        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-symbol-chat-preview-row {
                    display: flex;
                    justify-content: center;
                    padding: 8px 0;
                }
                .pw-symbol-chat-preview {
                    width: 288px;
                    height: 160px;
                    image-rendering: pixelated;
                    background: #181818;
                    border: 1px solid #444;
                    cursor: grab;
                    user-select: none;
                }
                .pw-symbol-chat-preview:active {
                    cursor: grabbing;
                }
                .pw-symbol-chat-part-row {
                    cursor: pointer;
                }
                .pw-symbol-chat-part-row-selected {
                    background: rgba(255, 255, 0, 0.15);
                }
                .pw-sega-palette {
                    display: inline-flex;
                    gap: 2px;
                }
                .pw-sega-swatch {
                    width: 14px;
                    height: 14px;
                    border: 1px solid #444;
                    cursor: pointer;
                }
                .pw-sega-swatch:hover {
                    border-color: #aaa;
                }
                .pw-sega-swatch-selected {
                    border: 2px solid #ffff00;
                    width: 12px;
                    height: 12px;
                }
                .pw-sega-picker {
                    position: relative;
                    display: inline-block;
                }
                .pw-sega-picker-preview {
                    width: 32px;
                    height: 32px;
                    border: 1px solid #444;
                    cursor: pointer;
                    background: #181818;
                    image-rendering: pixelated;
                }
                .pw-sega-picker-preview:hover {
                    border-color: #aaa;
                }
                .pw-sega-picker-popup {
                    position: absolute;
                    top: 100%;
                    left: 0;
                    z-index: 100;
                    display: grid;
                    grid-template-columns: repeat(8, 36px);
                    gap: 2px;
                    padding: 4px;
                    background: #222;
                    border: 1px solid #555;
                    box-shadow: 0 4px 12px rgba(0,0,0,0.6);
                    max-height: 320px;
                    overflow-y: auto;
                }
                .pw-sega-picker-cell {
                    width: 34px;
                    height: 34px;
                    border: 1px solid #444;
                    background: #181818;
                    cursor: pointer;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
                .pw-sega-picker-cell canvas {
                    image-rendering: pixelated;
                }
                .pw-sega-picker-cell:hover {
                    border-color: #ffff00;
                }
                .pw-sega-picker-cell-none {
                    border-color: #888;
                }
            """.trimIndent())
        }
    }

    private val isHexMode = labelType == DataLabelType.SymbolChatHexData
    /** Byte offset where the 60-byte SymbolChatT body starts. */
    private val bodyOffset = if (isHexMode) HEX_HEADER else 0
    private val totalSize = if (isHexMode) HEX_SIZE else SC_SIZE

    private val labels = ctrl.labelsOfType(labelType)
    private val selectedLabel = mutableCell<DataLabelEntry?>(null)

    // ---- HEX header fields (only meaningful when isHexMode) ----
    private val hexFaceType = mutableCell(0)
    private val hexFaceColor = mutableCell(0)
    private val hexSoundEffect = mutableCell(0)
    private val hexReserved = mutableCell(0)

    private val face = mutableCell(0)
    private val gcEndian = mutableCell(false)

    private var previewCanvas: HTMLCanvasElement? = null
    private var rendererReady = false

    /** Index of the part currently being highlighted/dragged, or -1. */
    private val selectedPart = mutableCell(-1)
    /** True while the mouse button is held down on a part. */
    private var dragging = false
    /** Offset (canvas px) from the part's top-left to where the user clicked. */
    private var dragOffsetX = 0
    private var dragOffsetY = 0

    private data class CornerCells(val icon: MutableCell<Int>, val param: MutableCell<Int>)
    private data class PartCells(
        val partId: MutableCell<Int>,
        val posX: MutableCell<Int>,
        val posY: MutableCell<Int>,
        val mirror: MutableCell<Int>,
    )

    private val corners = List(CORNERS) { CornerCells(mutableCell(0), mutableCell(0)) }
    private val parts = List(PARTS) {
        PartCells(mutableCell(0), mutableCell(0), mutableCell(0), mutableCell(0))
    }

    init {
        val bodyElement = dialogElement.querySelector(".pw-dialog-body")
        bodyElement?.let { body ->
            body.innerHTML = ""
            val contentWidget = addDisposable(Content())
            body.appendChild(contentWidget.element)
        }

        val footerElement = dialogElement.querySelector(".pw-dialog-footer")
        footerElement?.let { footer ->
            footer.innerHTML = ""
            val saveBtn = addDisposable(Button(
                text = "OK",
                enabled = map(ctrl.enabled, selectedLabel) { e, s -> e && s != null },
                onClick = { if (save()) onDismiss() },
            ))
            footer.appendChild(saveBtn.element)
            val cancelBtn = addDisposable(Button(text = "Cancel", onClick = { onDismiss() }))
            footer.appendChild(cancelBtn.element)
        }

        // Center the dialog in the viewport and clamp it to never exceed
        // the screen — internal scroll lives on the Content widget below.
        // Set top/right/bottom/left individually (not the `inset` shorthand
        // which Kotlin/JS doesn't always serialize) and use `margin: auto`
        // so a fixed-position box with a definite size centers in its
        // containing block (which after reparenting is the viewport).
        dialogElement.style.width = "460px"
        dialogElement.style.maxWidth = "94vw"
        dialogElement.style.maxHeight = "90vh"
        dialogElement.style.top = "0"
        dialogElement.style.right = "0"
        dialogElement.style.bottom = "0"
        dialogElement.style.left = "0"
        dialogElement.style.margin = "auto"

        // Ensure any open picker popup + its document listener are torn down
        // when the dialog is disposed (otherwise the listener leaks).
        addDisposable(disposable { closePopup() })

        observeNow(visible) { vis ->
            if (vis) {
                // The base Dialog class sets `transform: translate(x, y)` in
                // setPosition() with hard-coded 500×500 sizing assumptions
                // (Dialog.kt:78-83), which would push our 460px-wide,
                // 90vh-tall box off-center. Clear it so our `inset:0;
                // margin:auto` centering actually wins.
                dialogElement.style.transform = "none"
                mutateDeferred {
                    val targetId = initialLabelId.value
                    val entries = labels.value
                    val entry = if (targetId != null) {
                        entries.find { it.labelId == targetId }
                    } else {
                        entries.firstOrNull()
                    }
                    entry?.let(::loadLabel)
                }
                // Kick off atlas loading on first show; redraws once ready.
                SymbolChatRenderer.ensureLoaded {
                    rendererReady = true
                    redrawPreview()
                }
            } else {
                // Drop any open picker popup + its document listener when
                // the dialog hides — the popup is positioned absolutely off
                // a now-hidden node and its outside-click listener would
                // otherwise keep firing.
                closePopup()
            }
        }

        // Re-render the preview whenever any cell that feeds it changes.
        observeNow(face) { redrawPreview() }
        for (c in corners) {
            observeNow(c.icon) { redrawPreview() }
            observeNow(c.param) { redrawPreview() }
        }
        for (p in parts) {
            observeNow(p.partId) { redrawPreview() }
            observeNow(p.posX) { redrawPreview() }
            observeNow(p.posY) { redrawPreview() }
            observeNow(p.mirror) { redrawPreview() }
        }
        observeNow(selectedPart) { redrawPreview() }
    }

    /**
     * Translates a mouse event into internal canvas pixel coordinates,
     * accounting for CSS scaling (we render at 144×80 but display at 288×160).
     */
    private fun canvasCoords(canvas: HTMLCanvasElement, clientX: Double, clientY: Double): Pair<Int, Int> {
        val rect = canvas.getBoundingClientRect()
        val scaleX = canvas.width / rect.width
        val scaleY = canvas.height / rect.height
        val x = ((clientX - rect.left) * scaleX).toInt()
        val y = ((clientY - rect.top) * scaleY).toInt()
        return x to y
    }

    /**
     * Walks parts in reverse draw order (last drawn = on top) and returns the
     * index of the topmost part containing [point], or -1 if none.
     */
    private fun hitTestPart(point: Pair<Int, Int>): Int {
        for (i in parts.indices.reversed()) {
            val rect = SymbolChatRenderer.partRect(
                parts[i].partId.value and 0xFF,
                parts[i].posX.value and 0xFF,
                parts[i].posY.value and 0xFF,
            ) ?: continue
            if (point in rect) return i
        }
        return -1
    }

    private fun redrawPreview() {
        if (!rendererReady) return
        val canvas = previewCanvas ?: return
        SymbolChatRenderer.render(
            canvas = canvas,
            face = face.value,
            cornerIcons = IntArray(CORNERS) { corners[it].icon.value and 0xFF },
            cornerParams = IntArray(CORNERS) { corners[it].param.value and 0xFF },
            partIds = IntArray(PARTS) { parts[it].partId.value and 0xFF },
            partXs = IntArray(PARTS) { parts[it].posX.value and 0xFF },
            partYs = IntArray(PARTS) { parts[it].posY.value and 0xFF },
            partMirrors = IntArray(PARTS) { parts[it].mirror.value and 0xFF },
            highlightPart = selectedPart.value,
        )
    }

    private fun loadLabel(entry: DataLabelEntry) {
        selectedLabel.value = entry
        val buf = ctrl.readSegmentData(entry.labelId) ?: return
        if (buf.size < totalSize) return

        // ---- HEX header (bytes 0-3, only in hex mode) ----
        if (isHexMode) {
            hexFaceType.value = buf.getUByte(0).toInt()
            hexFaceColor.value = buf.getUByte(1).toInt()
            hexSoundEffect.value = buf.getUByte(2).toInt()
            hexReserved.value = buf.getUByte(3).toInt()
        }

        // ---- 60-byte SymbolChatT body (at bodyOffset) ----
        val o = bodyOffset
        val b0 = buf.getUByte(o).toInt()
        val b1 = buf.getUByte(o + 1).toInt()
        val b2 = buf.getUByte(o + 2).toInt()
        val b3 = buf.getUByte(o + 3).toInt()
        // BE detection heuristic: the face high two bytes are zero AND
        // the low two bytes (b2, b3 in file order) are not both zero.
        val gc = b0 == 0 && b1 == 0 && (b2 != 0 || b3 != 0)
        gcEndian.value = gc

        face.value = if (gc) {
            // big-endian: high byte first
            (b3) or (b2 shl 8) or (b1 shl 16) or (b0 shl 24)
        } else {
            (b0) or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
        }

        var off = o + 4
        for (c in corners) {
            val lo = buf.getUByte(off).toInt()
            val hi = buf.getUByte(off + 1).toInt()
            // corners is really a u16 — on GC the icon/param bytes are swapped.
            if (gc) {
                c.icon.value = hi
                c.param.value = lo
            } else {
                c.icon.value = lo
                c.param.value = hi
            }
            off += 2
        }
        for (p in parts) {
            // parts are byte arrays — same on LE and BE.
            p.partId.value = buf.getUByte(off).toInt(); off++
            p.posX.value = buf.getUByte(off).toInt(); off++
            p.posY.value = buf.getUByte(off).toInt(); off++
            p.mirror.value = buf.getUByte(off).toInt(); off++
        }
    }

    private fun save(): Boolean {
        val entry = selectedLabel.value ?: return false
        val buf = Buffer.withSize(totalSize, Endianness.Little)
        val gc = gcEndian.value

        // ---- HEX header (bytes 0-3, only in hex mode) ----
        if (isHexMode) {
            buf.setUByte(0, (hexFaceType.value and 0xFF).toUByte())
            buf.setUByte(1, (hexFaceColor.value and 0xFF).toUByte())
            buf.setUByte(2, (hexSoundEffect.value and 0xFF).toUByte())
            buf.setUByte(3, (hexReserved.value and 0xFF).toUByte())
        }

        // ---- 60-byte SymbolChatT body (at bodyOffset) ----
        val o = bodyOffset
        val f = face.value
        if (gc) {
            buf.setUByte(o, ((f ushr 24) and 0xFF).toUByte())
            buf.setUByte(o + 1, ((f ushr 16) and 0xFF).toUByte())
            buf.setUByte(o + 2, ((f ushr 8) and 0xFF).toUByte())
            buf.setUByte(o + 3, (f and 0xFF).toUByte())
        } else {
            buf.setUByte(o, (f and 0xFF).toUByte())
            buf.setUByte(o + 1, ((f ushr 8) and 0xFF).toUByte())
            buf.setUByte(o + 2, ((f ushr 16) and 0xFF).toUByte())
            buf.setUByte(o + 3, ((f ushr 24) and 0xFF).toUByte())
        }

        var off = o + 4
        for (c in corners) {
            val icon = (c.icon.value and 0xFF).toUByte()
            val param = (c.param.value and 0xFF).toUByte()
            if (gc) {
                buf.setUByte(off, param); off++
                buf.setUByte(off, icon);  off++
            } else {
                buf.setUByte(off, icon);  off++
                buf.setUByte(off, param); off++
            }
        }
        for (p in parts) {
            buf.setUByte(off, (p.partId.value and 0xFF).toUByte()); off++
            buf.setUByte(off, (p.posX.value and 0xFF).toUByte()); off++
            buf.setUByte(off, (p.posY.value and 0xFF).toUByte()); off++
            buf.setUByte(off, (p.mirror.value and 0xFF).toUByte()); off++
        }

        return ctrl.writeSegmentData(entry.labelId, buf)
    }

    private inner class Content : Widget() {
        override fun Node.createElement() =
            div {
                className = "pw-data-editor-content"
                // Cap our own height + scroll internally so the dialog footer
                // (OK / Cancel) stays visible regardless of viewport size.
                style.maxHeight = "calc(90vh - 120px)"
                style.overflowY = "auto"

                div {
                    className = "pw-data-editor-label-row"
                    span { textContent = "Label:" }
                    addChild(Select(
                        items = labels,
                        itemToString = { "Label ${it.labelId}" },
                        selected = selectedLabel,
                        onSelect = ::loadLabel,
                    ))
                    addChild(Checkbox(
                        label = "GC big-endian",
                        checked = gcEndian,
                        onChange = { gcEndian.value = it },
                    ))
                }

                // ---- HEX header panel (only in hex mode) ----------------
                if (isHexMode) {
                    div { textContent = "HEX Header" }
                    table {
                        className = "pw-data-editor-table"
                        tbody {
                            tr {
                                th { textContent = "Face Type" }
                                td { addChild(IntInput(
                                    value = hexFaceType,
                                    onChange = { hexFaceType.value = it },
                                    min = 0, max = 255,
                                )) }
                            }
                            tr {
                                th { textContent = "Face Color" }
                                td { addChild(IntInput(
                                    value = hexFaceColor,
                                    onChange = { hexFaceColor.value = it },
                                    min = 0, max = 255,
                                )) }
                            }
                            tr {
                                th { textContent = "Sound Effect" }
                                td { addChild(IntInput(
                                    value = hexSoundEffect,
                                    onChange = { hexSoundEffect.value = it },
                                    min = 0, max = 255,
                                )) }
                            }
                            tr {
                                th { textContent = "Unknown (byte 3)" }
                                td { addChild(IntInput(
                                    value = hexReserved,
                                    onChange = { hexReserved.value = it },
                                    min = 0, max = 255,
                                )) }
                            }
                        }
                    }
                }

                // Interactive preview canvas. Click a part to select it,
                // drag to reposition. The structured fields below stay
                // editable for everything else (face, corners, partId/mirror).
                div {
                    className = "pw-symbol-chat-preview-row"
                    previewCanvas = canvas {
                        className = "pw-symbol-chat-preview"
                        width = SymbolChatRenderer.CANVAS_WIDTH
                        height = SymbolChatRenderer.CANVAS_HEIGHT

                        onmousedown = { ev ->
                            val (cx, cy) = canvasCoords(this, ev.clientX.toDouble(), ev.clientY.toDouble())
                            val hit = hitTestPart(cx to cy)
                            if (hit >= 0) {
                                selectedPart.value = hit
                                val rect = SymbolChatRenderer.partRect(
                                    parts[hit].partId.value and 0xFF,
                                    parts[hit].posX.value and 0xFF,
                                    parts[hit].posY.value and 0xFF,
                                )!!
                                dragOffsetX = cx - rect.left
                                dragOffsetY = cy - rect.top
                                dragging = true
                                ev.preventDefault()
                            } else {
                                selectedPart.value = -1
                            }
                        }
                        onmousemove = { ev ->
                            if (dragging && selectedPart.value >= 0) {
                                val (cx, cy) = canvasCoords(this, ev.clientX.toDouble(), ev.clientY.toDouble())
                                val idx = selectedPart.value
                                val partId = parts[idx].partId.value and 0xFF
                                val w: Int; val h: Int
                                when {
                                    partId < 48 -> { w = 16; h = 16 }
                                    partId < 60 -> { w = 40; h = 20 }
                                    else        -> { w = 32; h = 32 }
                                }
                                // Invert the placement formula:
                                //   dstL = posX + 40 - w/2  →  posX = dstL - 40 + w/2
                                //   dstT = posY * 0.92      →  posY = dstT / 0.92
                                val newDstL = cx - dragOffsetX
                                val newDstT = cy - dragOffsetY
                                val newPosX = (newDstL - 40 + w / 2).coerceIn(0, 255)
                                val newPosY = (newDstT / 0.92).toInt().coerceIn(0, 255)
                                parts[idx].posX.value = newPosX
                                parts[idx].posY.value = newPosY
                            }
                        }
                        onmouseup = { _ -> dragging = false }
                        onmouseleave = { _ -> dragging = false }
                    }
                }
                // Trigger an initial render now that the canvas exists.
                redrawPreview()

                // ---- Face panel ---------------------------------------------
                div { textContent = "Face" }
                table {
                    className = "pw-data-editor-table"
                    tbody {
                        tr {
                            th { textContent = "Shape" }
                            td {
                                addChild(Select(
                                    items = cell((0..3).toList()),
                                    itemToString = { "Face $it" },
                                    selected = face.map { it and 3 },
                                    onSelect = { writeFaceShape(it) },
                                ))
                            }
                        }
                        tr {
                            th { textContent = "Color" }
                            td { addChild(SegaColorPalette(
                                selected = face.map { (it shr 2) and 7 },
                                onSelect = { writeFaceColor(it) },
                            )) }
                        }
                        tr {
                            th { textContent = "Extra" }
                            td { addChild(IntInput(
                                value = face.map { it ushr 5 },
                                onChange = { writeFaceExtra(it) },
                            )) }
                        }
                    }
                }

                // ---- Corner panel -------------------------------------------
                div { textContent = "Corners" }
                table {
                    className = "pw-data-editor-table"
                    tbody {
                        tr {
                            th {}
                            th { textContent = "Icon" }
                            th { textContent = "Color" }
                            th { textContent = "↔" }
                            th { textContent = "↕" }
                        }
                        for ((i, c) in corners.withIndex()) {
                            tr {
                                th { textContent = "C${i + 1}" }
                                td { addChild(SegaIconPicker(
                                    selected = c.icon,
                                    onChange = { c.icon.value = it },
                                )) }
                                td { addChild(SegaColorPalette(
                                    selected = c.param.map { it and 7 },
                                    onSelect = { writeCornerColor(i, it) },
                                )) }
                                td { addChild(Checkbox(
                                    checked = c.param.map { (it shr 3) and 1 == 1 },
                                    onChange = { writeCornerMirror(i, h = it, v = null) },
                                )) }
                                td { addChild(Checkbox(
                                    checked = c.param.map { (it shr 4) and 1 == 1 },
                                    onChange = { writeCornerMirror(i, h = null, v = it) },
                                )) }
                            }
                        }
                    }
                }

                // ---- Parts panel --------------------------------------------
                div { textContent = "Parts — click row or canvas to select; drag on canvas to reposition" }
                table {
                    className = "pw-data-editor-table"
                    tbody {
                        tr {
                            th {}
                            th { textContent = "Id" }
                            th { textContent = "X" }
                            th { textContent = "Y" }
                            th { textContent = "↔" }
                            th { textContent = "↕" }
                        }
                        for ((i, p) in parts.withIndex()) {
                            tr {
                                className = "pw-symbol-chat-part-row"
                                onclick = { selectedPart.value = i }
                                observeNow(selectedPart) { sel ->
                                    if (sel == i) classList.add("pw-symbol-chat-part-row-selected")
                                    else classList.remove("pw-symbol-chat-part-row-selected")
                                }
                                th { textContent = "P${i + 1}" }
                                td { addChild(SegaPartPicker(
                                    selected = p.partId,
                                    onChange = { p.partId.value = it },
                                )) }
                                td { addChild(IntInput(value = p.posX, onChange = { p.posX.value = it })) }
                                td { addChild(IntInput(value = p.posY, onChange = { p.posY.value = it })) }
                                td { addChild(Checkbox(
                                    checked = p.mirror.map { it and 1 == 1 },
                                    onChange = { writePartMirror(i, h = it, v = null) },
                                )) }
                                td { addChild(Checkbox(
                                    checked = p.mirror.map { (it shr 1) and 1 == 1 },
                                    onChange = { writePartMirror(i, h = null, v = it) },
                                )) }
                            }
                        }
                    }
                }
            }
    }

    // ---- bit-decomposition writers ------------------------------------------
    // The face dword and corner.param / part.mirror bytes pack multiple fields
    // into one int. Each writer reads the current value, masks out the field
    // being changed, and ORs the new field in.

    private fun writeFaceShape(shape: Int) {
        face.value = (face.value and 3.inv()) or (shape and 3)
    }
    private fun writeFaceColor(color: Int) {
        face.value = (face.value and (7 shl 2).inv()) or ((color and 7) shl 2)
    }
    private fun writeFaceExtra(extra: Int) {
        face.value = (face.value and ((1 shl 5) - 1)) or (extra shl 5)
    }
    private fun writeCornerColor(idx: Int, color: Int) {
        val cur = corners[idx].param.value
        corners[idx].param.value = (cur and 7.inv()) or (color and 7)
    }
    /** Pass null to leave a field unchanged. */
    private fun writeCornerMirror(idx: Int, h: Boolean?, v: Boolean?) {
        var p = corners[idx].param.value
        if (h != null) p = (p and (1 shl 3).inv()) or (if (h) 1 shl 3 else 0)
        if (v != null) p = (p and (1 shl 4).inv()) or (if (v) 1 shl 4 else 0)
        corners[idx].param.value = p
    }
    private fun writePartMirror(idx: Int, h: Boolean?, v: Boolean?) {
        var m = parts[idx].mirror.value
        if (h != null) m = (m and 1.inv()) or (if (h) 1 else 0)
        if (v != null) m = (m and (1 shl 1).inv()) or (if (v) 1 shl 1 else 0)
        parts[idx].mirror.value = m
    }

    /**
     * Inline picker that shows the currently-selected sega icon as a 32×32
     * thumbnail and pops up an 8-column grid of all 192 icons + a "none"
     * cell on click. One picker per corner row — embedding the grid in each
     * row removes the need to first pick which corner is active.
     */
    private inner class SegaIconPicker(
        private val selected: MutableCell<Int>,
        private val onChange: (Int) -> Unit,
    ) : Widget() {
        override fun Node.createElement() =
            div {
                className = "pw-sega-picker"

                val previewCanvas = canvas {
                    className = "pw-sega-picker-preview"
                    width = 32; height = 32
                }
                val updatePreview = {
                    SymbolChatRenderer.drawIconTile(previewCanvas, selected.value and 0xFF)
                }
                observeNow(selected) { updatePreview() }
                SymbolChatRenderer.ensureLoaded { updatePreview() }

                val popup = div {
                    className = "pw-sega-picker-popup"
                    style.display = "none"

                    // "none" cell
                    addPickerCell(this, isNone = true) {
                        SymbolChatRenderer.drawIconTile(it, 0xFF)
                    }.onclick = { _ ->
                        onChange(0xFF); selected.value = 0xFF; closePopup()
                    }

                    for (id in 0 until SymbolChatRenderer.ICON_COUNT) {
                        val cell = addPickerCell(this, isNone = false) {
                            SymbolChatRenderer.drawIconTile(it, id)
                        }
                        cell.title = "Icon $id"
                        cell.onclick = { _ ->
                            onChange(id); selected.value = id; closePopup()
                        }
                    }
                }

                previewCanvas.onclick = { _ ->
                    if (openedPopup === popup) closePopup() else openPopup(popup)
                }
            }
    }

    /**
     * Same shape as [SegaIconPicker] but for the 68 part shapes (sega_3 atlas).
     */
    private inner class SegaPartPicker(
        private val selected: MutableCell<Int>,
        private val onChange: (Int) -> Unit,
    ) : Widget() {
        override fun Node.createElement() =
            div {
                className = "pw-sega-picker"

                val previewCanvas = canvas {
                    className = "pw-sega-picker-preview"
                    width = 32; height = 32
                }
                val updatePreview = {
                    SymbolChatRenderer.drawPartTile(previewCanvas, selected.value and 0xFF)
                }
                observeNow(selected) { updatePreview() }
                SymbolChatRenderer.ensureLoaded { updatePreview() }

                val popup = div {
                    className = "pw-sega-picker-popup"
                    style.display = "none"

                    addPickerCell(this, isNone = true) {
                        SymbolChatRenderer.drawPartTile(it, 0xFF)
                    }.onclick = { _ ->
                        onChange(0xFF); selected.value = 0xFF; closePopup()
                    }

                    for (id in 0 until SymbolChatRenderer.PART_COUNT) {
                        val cell = addPickerCell(this, isNone = false) {
                            SymbolChatRenderer.drawPartTile(it, id)
                        }
                        cell.title = "Part $id"
                        cell.onclick = { _ ->
                            onChange(id); selected.value = id; closePopup()
                        }
                    }
                }

                previewCanvas.onclick = { _ ->
                    if (openedPopup === popup) closePopup() else openPopup(popup)
                }
            }
    }

    /** Helper to append one 32×32 cell with a canvas inside. */
    private fun addPickerCell(parent: Element, isNone: Boolean, draw: (HTMLCanvasElement) -> Unit): HTMLDivElement {
        val cell = document.createElement("div") as HTMLDivElement
        cell.className = if (isNone) "pw-sega-picker-cell pw-sega-picker-cell-none" else "pw-sega-picker-cell"
        val cnv = document.createElement("canvas") as HTMLCanvasElement
        cell.appendChild(cnv)
        parent.appendChild(cell)
        // Draw immediately if atlases are ready, and rerun after load.
        SymbolChatRenderer.ensureLoaded { draw(cnv) }
        draw(cnv)
        return cell
    }

    /** Currently-open popup, tracked so a single document listener can close it. */
    private var openedPopup: HTMLDivElement? = null
    private var popupOutsideListener: ((Event) -> Unit)? = null

    private fun openPopup(popup: HTMLDivElement) {
        closePopup()
        popup.style.display = "grid"
        openedPopup = popup

        // Close on next outside click. Defer attach so the click that opened
        // the popup doesn't immediately close it. If the dialog is disposed
        // (or another popup opened) before the 0ms fires, `openedPopup` will
        // have been cleared/changed — skip the attach so we don't leak a
        // listener onto document.
        window.setTimeout({
            if (openedPopup !== popup) return@setTimeout
            val listener: (Event) -> Unit = { ev ->
                val target = ev.target as? Node
                if (target == null || !popup.contains(target)) {
                    closePopup()
                }
            }
            popupOutsideListener = listener
            document.addEventListener("mousedown", listener)
        }, 0)
    }

    private fun closePopup() {
        openedPopup?.style?.display = "none"
        openedPopup = null
        popupOutsideListener?.let {
            document.removeEventListener("mousedown", it)
        }
        popupOutsideListener = null
    }

    /**
     * Tiny widget that renders the 9-colour symbol-chat palette as
     * clickable swatches. The currently-selected colour is outlined.
     */
    private inner class SegaColorPalette(
        private val selected: Cell<Int>,
        private val onSelect: (Int) -> Unit,
    ) : Widget() {
        override fun Node.createElement() =
            div {
                className = "pw-sega-palette"
                for (i in 0..8) {
                    div {
                        className = "pw-sega-swatch"
                        style.background = "#" + SEGA_COLOR_HEX[i]
                        title = "Color $i"
                        onclick = { onSelect(i) }
                        observeNow(selected) { sel ->
                            if (sel == i) classList.add("pw-sega-swatch-selected")
                            else classList.remove("pw-sega-swatch-selected")
                        }
                    }
                }
            }
    }
}

private val SEGA_COLOR_HEX = arrayOf(
    "CFCFCF", "3C50F3", "2FA3FF", "F0F000", "79FD79",
    "88A8E8", "B492D1", "787878", "FFFFFF",
)
