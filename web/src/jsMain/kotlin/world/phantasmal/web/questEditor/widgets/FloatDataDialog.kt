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
 * Edits a labelled float-array data segment.
 *
 * Float data has no opcode-based detection in PSO bytecode, so labels are
 * shown only after the user marks them via "Mark label as: Float" in the
 * editor context menu (see [DataEditorController.setLabelTypeOverride]).
 *
 * Values are interpreted as little-endian Float32. One float per line in
 * the textarea — this trivially supports the variable-length nature of
 * float arrays without a dynamic input list. On save, lines are parsed
 * back into a fresh Buffer of size N*4.
 */
class FloatDataDialog(
    visible: Cell<Boolean>,
    private val ctrl: DataEditorController,
    onDismiss: () -> Unit,
    private val initialLabelId: Cell<Int?> = cell(null),
) : Dialog(
    visible = visible,
    title = cell("Edit Float Data"),
    description = cell("One float per line. Stored as little-endian Float32."),
    content = {},
    footer = {},
    onDismiss = onDismiss,
) {
    private val labels = ctrl.labelsOfType(DataLabelType.FloatData)
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

        dialogElement.style.width = "320px"
        dialogElement.style.maxHeight = "560px"

        // Auto-select label when dialog opens. See EnemyPhysicalDataDialog for
        // why mutateDeferred is required here.
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
        // Round down — trailing partial float (1..3 leftover bytes) is dropped
        // for display, but preserved on save only if user doesn't edit.
        val count = buf.size / 4
        val sb = StringBuilder()
        for (i in 0 until count) {
            if (i > 0) sb.append('\n')
            sb.append(buf.getFloat(i * 4).toString())
        }
        text.value = sb.toString()
    }

    private fun save(): Boolean {
        val entry = selectedLabel.value ?: return false
        val lines = text.value.split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val floats = FloatArray(lines.size)
        for ((i, line) in lines.withIndex()) {
            val v = line.toFloatOrNull()
            if (v == null) {
                val msg = "Line ${i + 1} is not a valid float: \"$line\""
                logger.warn { "FloatDataDialog: $msg" }
                errorMessage.value = msg
                return false
            }
            floats[i] = v
        }

        errorMessage.value = ""
        val buf = Buffer.withSize(floats.size * 4, Endianness.Little)
        for (i in floats.indices) buf.setFloat(i * 4, floats[i])
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
                    cols = 30,
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
