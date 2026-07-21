package world.phantasmal.web.questEditor.widgets

import org.w3c.dom.Node
import world.phantasmal.cell.Cell
import world.phantasmal.cell.cell
import world.phantasmal.cell.map
import world.phantasmal.cell.mutableCell
import world.phantasmal.cell.mutateDeferred
import world.phantasmal.psolib.asm.EnemyMovementData
import world.phantasmal.web.questEditor.asm.DataLabelType
import world.phantasmal.web.questEditor.controllers.DataEditorController
import world.phantasmal.web.questEditor.controllers.DataLabelEntry
import world.phantasmal.webui.dom.*
import world.phantasmal.webui.widgets.*

class EnemyMovementDataDialog(
    visible: Cell<Boolean>,
    private val ctrl: DataEditorController,
    onDismiss: () -> Unit,
    private val initialLabelId: Cell<Int?> = cell(null),
) : Dialog(
    visible = visible,
    title = cell("Edit Enemy Movement Data"),
    description = cell("Edit MovementData (get_movement_data)"),
    content = {},
    footer = {},
    onDismiss = onDismiss,
) {
    private val labels = ctrl.labelsOfType(DataLabelType.MovementData)
    private val selectedLabel = mutableCell<DataLabelEntry?>(null)

    private val f1 = mutableCell(0.0)
    private val f2 = mutableCell(0.0)
    private val f3 = mutableCell(0.0)
    private val f4 = mutableCell(0.0)
    private val f5 = mutableCell(0.0)
    private val f6 = mutableCell(0.0)
    private val i1 = mutableCell(0)
    private val i2 = mutableCell(0)
    private val i3 = mutableCell(0)
    private val i4 = mutableCell(0)
    private val i5 = mutableCell(0)
    private val i6 = mutableCell(0)

    private val templateDialogVisible = mutableCell(false)

    init {
        val bodyElement = dialogElement.querySelector(".pw-dialog-body")
        bodyElement?.let { body ->
            body.innerHTML = ""
            val contentWidget = addDisposable(Content())
            body.appendChild(contentWidget.element)
        }

        addDisposable(LoadTemplateDialog(
            visible = templateDialogVisible,
            repo = ctrl.battleParamRepository,
            kind = LoadTemplateDialog.TemplateKind.Movement,
            onApply = ::applyTemplate,
            onDismiss = { templateDialogVisible.value = false },
        ))

        val footerElement = dialogElement.querySelector(".pw-dialog-footer")
        footerElement?.let { footer ->
            footer.innerHTML = ""
            val loadBtn = addDisposable(Button(
                text = "Load template…",
                enabled = ctrl.battleParamRepository.available,
                onClick = { templateDialogVisible.value = true },
            ))
            footer.appendChild(loadBtn.element)
            val saveBtn = addDisposable(Button(
                text = "OK",
                enabled = map(ctrl.enabled, selectedLabel) { e, s -> e && s != null },
                onClick = { save(); onDismiss() },
            ))
            footer.appendChild(saveBtn.element)
            val cancelBtn = addDisposable(Button(text = "Cancel", onClick = { onDismiss() }))
            footer.appendChild(cancelBtn.element)
        }

        dialogElement.style.width = "400px"
        dialogElement.style.maxHeight = "500px"

        // Auto-select label when dialog opens.
        // See EnemyPhysicalDataDialog for why mutateDeferred is required here.
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
        val buf = ctrl.readSegmentData(entry.labelId) ?: return
        if (buf.size < EnemyMovementData.SIZE) return
        val data = EnemyMovementData.readFrom(buf)

        f1.value = data.f1.toDouble()
        f2.value = data.f2.toDouble()
        f3.value = data.f3.toDouble()
        f4.value = data.f4.toDouble()
        f5.value = data.f5.toDouble()
        f6.value = data.f6.toDouble()
        i1.value = data.i1
        i2.value = data.i2
        i3.value = data.i3
        i4.value = data.i4
        i5.value = data.i5
        i6.value = data.i6
    }

    private fun save() {
        val entry = selectedLabel.value ?: return
        val buf = ctrl.readSegmentData(entry.labelId) ?: return
        if (buf.size < EnemyMovementData.SIZE) return

        val data = EnemyMovementData(
            f1 = f1.value.toFloat(),
            f2 = f2.value.toFloat(),
            f3 = f3.value.toFloat(),
            f4 = f4.value.toFloat(),
            f5 = f5.value.toFloat(),
            f6 = f6.value.toFloat(),
            i1 = i1.value,
            i2 = i2.value,
            i3 = i3.value,
            i4 = i4.value,
            i5 = i5.value,
            i6 = i6.value,
        )
        data.writeTo(buf)
        ctrl.writeSegmentData(entry.labelId, buf)
    }

    private fun applyTemplate(lookup: LoadTemplateDialog.TemplateLookup) {
        val data = lookup.table.movement(lookup.difficulty, lookup.slot)
        f1.value = data.f1.toDouble()
        f2.value = data.f2.toDouble()
        f3.value = data.f3.toDouble()
        f4.value = data.f4.toDouble()
        f5.value = data.f5.toDouble()
        f6.value = data.f6.toDouble()
        i1.value = data.i1
        i2.value = data.i2
        i3.value = data.i3
        i4.value = data.i4
        i5.value = data.i5
        i6.value = data.i6
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

                table {
                    className = "pw-data-editor-table"

                    tbody {
                        fieldRow("NNF1", DoubleInput(value = f1,
                            onChange = { f1.value = it }, roundTo = 4))
                        fieldRow("NNF2", DoubleInput(value = f2,
                            onChange = { f2.value = it }, roundTo = 4))
                        fieldRow("NNF3", DoubleInput(value = f3,
                            onChange = { f3.value = it }, roundTo = 4))
                        fieldRow("NNF4", DoubleInput(value = f4,
                            onChange = { f4.value = it }, roundTo = 4))
                        fieldRow("NNF5", DoubleInput(value = f5,
                            onChange = { f5.value = it }, roundTo = 4))
                        fieldRow("NNF6", DoubleInput(value = f6,
                            onChange = { f6.value = it }, roundTo = 4))
                        fieldRow("NNI1", IntInput(value = i1,
                            onChange = { i1.value = it }))
                        fieldRow("NNI2", IntInput(value = i2,
                            onChange = { i2.value = it }))
                        fieldRow("NNI3", IntInput(value = i3,
                            onChange = { i3.value = it }))
                        fieldRow("NNI4", IntInput(value = i4,
                            onChange = { i4.value = it }))
                        fieldRow("NNI5", IntInput(value = i5,
                            onChange = { i5.value = it }))
                        fieldRow("NNI6", IntInput(value = i6,
                            onChange = { i6.value = it }))
                    }
                }
            }

        private fun <T> Node.fieldRow(labelText: String, input: Input<T>) {
            val inputWidget = input
            tr {
                th {
                    val labelWidget = inputWidget.label
                    if (labelWidget != null) {
                        addChild(labelWidget)
                    } else {
                        textContent = labelText
                    }
                }
                td { addChild(inputWidget) }
            }
        }
    }
}
