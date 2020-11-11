package world.phantasmal.webui.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.HTMLInputElement
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.nullVal
import world.phantasmal.observable.value.trueVal

class IntInput(
    scope: CoroutineScope,
    visible: Val<Boolean> = trueVal(),
    enabled: Val<Boolean> = trueVal(),
    tooltip: Val<String?> = nullVal(),
    label: String? = null,
    labelVal: Val<String>? = null,
    preferredLabelPosition: LabelPosition = LabelPosition.Before,
    value: Int? = null,
    valueVal: Val<Int>? = null,
    onChange: (Int) -> Unit = {},
    min: Int? = null,
    max: Int? = null,
    step: Int? = null,
) : NumberInput<Int>(
    scope,
    visible,
    enabled,
    tooltip,
    label,
    labelVal,
    preferredLabelPosition,
    value,
    valueVal,
    onChange,
    min,
    max,
    step,
) {
    override fun getInputValue(input: HTMLInputElement): Int = input.valueAsNumber.toInt()

    override fun setInputValue(input: HTMLInputElement, value: Int) {
        input.valueAsNumber = value.toDouble()
    }
}
