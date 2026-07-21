package world.phantasmal.web.questEditor.widgets

import org.w3c.dom.Node
import world.phantasmal.cell.Cell
import world.phantasmal.cell.cell
import world.phantasmal.cell.map
import world.phantasmal.cell.mutableCell
import mu.KotlinLogging
import world.phantasmal.cell.mutateDeferred
import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.buffer.Buffer
import world.phantasmal.web.questEditor.asm.DataLabelType
import world.phantasmal.web.questEditor.controllers.DataEditorController
import world.phantasmal.web.questEditor.controllers.DataLabelEntry
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.dom.span
import world.phantasmal.webui.widgets.Button
import world.phantasmal.webui.widgets.Dialog
import world.phantasmal.webui.widgets.Select
import world.phantasmal.webui.widgets.TextArea
import world.phantasmal.webui.widgets.Widget

private val logger = KotlinLogging.logger {}

/**
 * Edits a labelled vector-list data segment.
 *
 * Each record is 16 bytes: x, y, z, duration (all little-endian Float32).
 * Used by `get_vector_from_path` (0xf8db) and `compute_bezier_curve_point`
 * (0xf8f2) — both reference the segment via a trailing dlabel arg, so
 * vector labels are auto-detected. Users can also force the type via
 * "Mark label as: Vector".
 *
 * Format: one record per line, four space-separated floats — `x y z duration`.
 */
class VectorDataDialog(
    visible: Cell<Boolean>,
    private val ctrl: DataEditorController,
    onDismiss: () -> Unit,
    private val initialLabelId: Cell<Int?> = cell(null),
) : Dialog(
    visible = visible,
    title = cell("Edit Vector Data"),
    description = cell("One record per line: x y z duration (little-endian Float32)."),
    content = {},
    footer = {},
    onDismiss = onDismiss,
) {
    private val labels = ctrl.labelsOfType(DataLabelType.VectorData)
    private val selectedLabel = mutableCell<DataLabelEntry?>(null)
    private val text = mutableCell("")
    private val errorMessage = mutableCell("")

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

        dialogElement.style.width = "420px"
        dialogElement.style.maxHeight = "560px"

        observeNow(visible) { vis ->
            if (vis) {
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
            }
        }
    }

    private fun loadLabel(entry: DataLabelEntry) {
        selectedLabel.value = entry
        errorMessage.value = ""
        val buf = ctrl.readSegmentData(entry.labelId)
        if (buf == null) {
            text.value = ""
            return
        }
        val count = buf.size / 16
        val sb = StringBuilder()
        for (i in 0 until count) {
            if (i > 0) sb.append('\n')
            val x = buf.getFloat(i * 16)
            val y = buf.getFloat(i * 16 + 4)
            val z = buf.getFloat(i * 16 + 8)
            val d = buf.getFloat(i * 16 + 12)
            sb.append(x).append(' ').append(y).append(' ').append(z).append(' ').append(d)
        }
        text.value = sb.toString()
    }

    private fun save(): Boolean {
        val entry = selectedLabel.value ?: return false
        val records = mutableListOf<FloatArray>()
        for ((idx, rawLine) in text.value.split('\n').withIndex()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val parts = line.split(Regex("\\s+"))
            if (parts.size != 4) {
                val msg = "Line ${idx + 1} expected 4 floats, got ${parts.size}: \"$line\""
                logger.warn { "VectorDataDialog: $msg" }
                errorMessage.value = msg
                return false
            }
            val rec = FloatArray(4)
            for (i in 0..3) {
                val v = parts[i].toFloatOrNull()
                if (v == null) {
                    val msg = "Line ${idx + 1} field ${i + 1} is not a valid float: \"${parts[i]}\""
                    logger.warn { "VectorDataDialog: $msg" }
                    errorMessage.value = msg
                    return false
                }
                rec[i] = v
            }
            records.add(rec)
        }
        errorMessage.value = ""

        val buf = Buffer.withSize(records.size * 16, Endianness.Little)
        for ((i, rec) in records.withIndex()) {
            buf.setFloat(i * 16,      rec[0])
            buf.setFloat(i * 16 + 4,  rec[1])
            buf.setFloat(i * 16 + 8,  rec[2])
            buf.setFloat(i * 16 + 12, rec[3])
        }
        return ctrl.writeSegmentData(entry.labelId, buf)
    }

    private inner class Content : Widget() {
        override fun Node.createElement() =
            div {
                className = "pw-data-editor-content"

                div {
                    className = "pw-data-editor-label-row"
                    span { textContent = "Label:" }
                    addChild(Select(
                        items = labels,
                        itemToString = { "Label ${it.labelId}" },
                        selected = selectedLabel,
                        onSelect = ::loadLabel,
                    ))
                }

                addChild(TextArea(
                    value = text,
                    onChange = { text.value = it; errorMessage.value = "" },
                    rows = 18,
                    cols = 42,
                    fontFamily = "monospace",
                    triggerOnInput = true,
                ))

                div {
                    className = "pw-data-editor-error"
                    observeNow(errorMessage) { textContent = it }
                }
            }
    }
}
