package world.phantasmal.webui.widgets

import org.w3c.dom.Node
import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.nullCell
import world.phantasmal.observable.cell.trueCell
import world.phantasmal.webui.dom.input

class Checkbox(
    visible: Cell<Boolean> = trueCell(),
    enabled: Cell<Boolean> = trueCell(),
    tooltip: Cell<String?> = nullCell(),
    label: String? = null,
    labelCell: Cell<String>? = null,
    preferredLabelPosition: LabelPosition = LabelPosition.After,
    private val checked: Cell<Boolean>? = null,
    private val onChange: ((Boolean) -> Unit)? = null,
) : LabelledControl(visible, enabled, tooltip, label, labelCell, preferredLabelPosition) {
    override fun Node.createElement() =
        input {
            id = labelId
            className = "pw-checkbox"
            type = "checkbox"

            if (this@Checkbox.checked != null) {
                observe(this@Checkbox.checked) {
                    checked = it
                }
            }

            if (onChange != null) {
                onchange = { onChange.invoke(checked) }
            }
        }
}
