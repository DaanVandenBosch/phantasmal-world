package world.phantasmal.webui.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.HTMLInputElement
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal

class TextInput(
    scope: CoroutineScope,
    hidden: Val<Boolean> = falseVal(),
    disabled: Val<Boolean> = falseVal(),
    tooltip: String? = null,
    label: String? = null,
    labelVal: Val<String>? = null,
    preferredLabelPosition: LabelPosition = LabelPosition.Before,
    value: String? = null,
    valueVal: Val<String>? = null,
    onChange: (String) -> Unit = {},
    maxLength: Int? = null,
) : Input<String>(
    scope,
    hidden,
    disabled,
    tooltip,
    label,
    labelVal,
    preferredLabelPosition,
    className = "pw-text-input",
    inputClassName = "pw-number-text-inner",
    inputType = "text",
    value,
    valueVal,
    onChange,
    maxLength,
    min = null,
    max = null,
    step = null
) {
    override fun getInputValue(input: HTMLInputElement): String = input.value

    override fun setInputValue(input: HTMLInputElement, value: String) {
        input.value = value
    }
}
