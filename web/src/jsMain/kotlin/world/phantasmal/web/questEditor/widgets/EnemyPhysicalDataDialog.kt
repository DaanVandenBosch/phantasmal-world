package world.phantasmal.web.questEditor.widgets

import org.w3c.dom.Node
import world.phantasmal.cell.Cell
import world.phantasmal.cell.cell
import world.phantasmal.cell.map
import world.phantasmal.cell.mutableCell
import world.phantasmal.cell.mutateDeferred
import world.phantasmal.psolib.asm.EnemyPhysicalData
import world.phantasmal.web.questEditor.asm.DataLabelType
import world.phantasmal.web.questEditor.controllers.DataEditorController
import world.phantasmal.web.questEditor.controllers.DataLabelEntry
import world.phantasmal.webui.dom.*
import world.phantasmal.webui.widgets.*

class EnemyPhysicalDataDialog(
    visible: Cell<Boolean>,
    private val ctrl: DataEditorController,
    onDismiss: () -> Unit,
    private val initialLabelId: Cell<Int?> = cell(null),
) : Dialog(
    visible = visible,
    title = cell("Edit Enemy Physical Data"),
    description = cell("Edit PlayerStats (get_physical_data)"),
    content = {},
    footer = {},
    onDismiss = onDismiss,
) {
    private val labels = ctrl.labelsOfType(DataLabelType.PhysicalData)
    private val selectedLabel = mutableCell<DataLabelEntry?>(null)

    private val atp = mutableCell(0)
    private val mst = mutableCell(0)
    private val evp = mutableCell(0)
    private val hp = mutableCell(0)
    private val dfp = mutableCell(0)
    private val ata = mutableCell(0)
    private val lck = mutableCell(0)
    private val esp = mutableCell(0)
    private val attackRange = mutableCell(0.0)
    private val knockbackRange = mutableCell(0.0)
    private val level = mutableCell(0)
    private val experience = mutableCell(0)
    private val meseta = mutableCell(0)

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
            kind = LoadTemplateDialog.TemplateKind.Physical,
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

        dialogElement.style.width = "450px"
        dialogElement.style.maxHeight = "550px"

        // Auto-select label when dialog opens.
        // The observer fires inside the cell-mutation notification phase, so any
        // direct cell writes (loadLabel mutates ~13 cells) would re-enter the
        // mutation loop and silently lose updates. Defer them to a fresh
        // mutation that runs after the current notification phase finishes.
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
        if (buf.size < EnemyPhysicalData.SIZE) return
        val data = EnemyPhysicalData.readFrom(buf)

        atp.value = data.atp.toInt()
        mst.value = data.mst.toInt()
        evp.value = data.evp.toInt()
        hp.value = data.hp.toInt()
        dfp.value = data.dfp.toInt()
        ata.value = data.ata.toInt()
        lck.value = data.lck.toInt()
        esp.value = data.esp.toInt()
        attackRange.value = data.attackRange.toDouble()
        knockbackRange.value = data.knockbackRange.toDouble()
        level.value = data.level.toInt()
        experience.value = data.experience.toInt()
        meseta.value = data.meseta.toInt()
    }

    private fun save() {
        val entry = selectedLabel.value ?: return
        val buf = ctrl.readSegmentData(entry.labelId) ?: return
        if (buf.size < EnemyPhysicalData.SIZE) return

        val data = EnemyPhysicalData(
            atp = atp.value.toUShort(),
            mst = mst.value.toUShort(),
            evp = evp.value.toUShort(),
            hp = hp.value.toUShort(),
            dfp = dfp.value.toUShort(),
            ata = ata.value.toUShort(),
            lck = lck.value.toUShort(),
            esp = esp.value.toUShort(),
            attackRange = attackRange.value.toFloat(),
            knockbackRange = knockbackRange.value.toFloat(),
            level = level.value.toUInt(),
            experience = experience.value.toUInt(),
            meseta = meseta.value.toUInt(),
        )
        data.writeTo(buf)
        ctrl.writeSegmentData(entry.labelId, buf)
    }

    private fun applyTemplate(lookup: LoadTemplateDialog.TemplateLookup) {
        val data = lookup.table.physical(lookup.difficulty, lookup.slot)
        atp.value = data.atp.toInt()
        mst.value = data.mst.toInt()
        evp.value = data.evp.toInt()
        hp.value = data.hp.toInt()
        dfp.value = data.dfp.toInt()
        ata.value = data.ata.toInt()
        lck.value = data.lck.toInt()
        esp.value = data.esp.toInt()
        attackRange.value = data.attackRange.toDouble()
        knockbackRange.value = data.knockbackRange.toDouble()
        level.value = data.level.toInt()
        experience.value = data.experience.toInt()
        meseta.value = data.meseta.toInt()
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
                        // Primary stats
                        fieldRow("ATP", IntInput(value = atp,
                            onChange = { atp.value = it }, min = 0, max = 65535))
                        fieldRow("MST", IntInput(value = mst,
                            onChange = { mst.value = it }, min = 0, max = 65535))
                        fieldRow("ATA", IntInput(value = ata,
                            onChange = { ata.value = it }, min = 0, max = 65535))
                        fieldRow("DFP", IntInput(value = dfp,
                            onChange = { dfp.value = it }, min = 0, max = 65535))
                        fieldRow("HP", IntInput(value = hp,
                            onChange = { hp.value = it }, min = 0, max = 65535))
                        // Secondary stats
                        fieldRow("EVP", IntInput(value = evp,
                            onChange = { evp.value = it }, min = 0, max = 65535))
                        fieldRow("ESP", IntInput(value = esp,
                            onChange = { esp.value = it }, min = 0, max = 65535))
                        fieldRow("LCK", IntInput(value = lck,
                            onChange = { lck.value = it }, min = 0, max = 65535))
                        fieldRow("TP", IntInput(value = meseta,
                            onChange = { meseta.value = it }, min = 0))
                        fieldRow("EXP", IntInput(value = experience,
                            onChange = { experience.value = it }, min = 0))
                        // Range / tech
                        fieldRow("Range", DoubleInput(value = attackRange,
                            onChange = { attackRange.value = it }, roundTo = 4))
                        fieldRow("Knockback range", DoubleInput(value = knockbackRange,
                            onChange = { knockbackRange.value = it }, roundTo = 4))
                        fieldRow("Tech", IntInput(value = level,
                            onChange = { level.value = it }, min = 0, max = 29))
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
