package world.phantasmal.webui.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.HTMLInputElement
import world.phantasmal.observable.value.*

class TextInput(
    scope: CoroutineScope,
    visible: Val<Boolean> = trueVal(),
    enabled: Val<Boolean> = trueVal(),
    tooltip: Val<String?> = nullVal(),
    label: String? = null,
    labelVal: Val<String>? = null,
    preferredLabelPosition: LabelPosition = LabelPosition.Before,
    value: Val<String> = emptyStringVal(),
    onChange: (String) -> Unit = {},
    maxLength: Int? = null,
) : Input<String>(
    scope,
    visible,
    enabled,
    tooltip,
    label,
    labelVal,
    preferredLabelPosition,
    className = "pw-text-input",
    inputClassName = "pw-number-text-inner",
    inputType = "text",
    value,
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
