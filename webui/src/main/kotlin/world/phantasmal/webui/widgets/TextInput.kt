package world.phantasmal.webui.widgets

import org.w3c.dom.HTMLInputElement
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.emptyStringVal
import world.phantasmal.observable.value.nullVal
import world.phantasmal.observable.value.trueVal

class TextInput(
    visible: Val<Boolean> = trueVal(),
    enabled: Val<Boolean> = trueVal(),
    tooltip: Val<String?> = nullVal(),
    label: String? = null,
    labelVal: Val<String>? = null,
    preferredLabelPosition: LabelPosition = LabelPosition.Before,
    value: Val<String> = emptyStringVal(),
    onChange: (String) -> Unit = {},
    private val maxLength: Int? = null,
) : Input<String>(
    visible,
    enabled,
    tooltip,
    label,
    labelVal,
    preferredLabelPosition,
    className = "pw-text-input",
    value,
    onChange,
) {
    override fun interceptInputElement(input: HTMLInputElement) {
        super.interceptInputElement(input)

        input.type = "text"
        maxLength?.let { input.maxLength = it }
    }

    override fun getInputValue(input: HTMLInputElement): String = input.value

    override fun setInputValue(input: HTMLInputElement, value: String) {
        input.value = value
    }
}
