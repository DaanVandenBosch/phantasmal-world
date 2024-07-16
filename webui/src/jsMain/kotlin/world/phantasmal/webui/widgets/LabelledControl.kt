package world.phantasmal.webui.widgets

import world.phantasmal.cell.Cell

enum class LabelPosition {
    Before,
    After
}

abstract class LabelledControl(
    visible: Cell<Boolean>,
    enabled: Cell<Boolean>,
    tooltip: Cell<String?>,
    label: String?,
    labelCell: Cell<String>?,
    val preferredLabelPosition: LabelPosition,
) : Control(visible, enabled, tooltip) {
    protected val labelId: String = uniqueId()

    val label: Label? by lazy {
        if (label == null && labelCell == null) {
            null
        } else {
            Label(visible, enabled, label, labelCell, htmlFor = labelId)
        }
    }
}
