package world.phantasmal.webui.widgets

import org.w3c.dom.Node
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.nullVal
import world.phantasmal.observable.value.trueVal
import world.phantasmal.webui.dom.input

class Checkbox(
    visible: Val<Boolean> = trueVal(),
    enabled: Val<Boolean> = trueVal(),
    tooltip: Val<String?> = nullVal(),
    label: String? = null,
    labelVal: Val<String>? = null,
    preferredLabelPosition: LabelPosition = LabelPosition.After,
    private val checked: Val<Boolean>? = null,
    private val onChange: ((Boolean) -> Unit)? = null,
) : LabelledControl(visible, enabled, tooltip, label, labelVal, preferredLabelPosition) {
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
