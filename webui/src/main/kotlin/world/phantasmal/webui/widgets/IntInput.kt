package world.phantasmal.webui.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.HTMLInputElement
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal

class IntInput(
    scope: CoroutineScope,
    hidden: Val<Boolean> = falseVal(),
    disabled: Val<Boolean> = falseVal(),
    label: String? = null,
    labelVal: Val<String>? = null,
    preferredLabelPosition: LabelPosition = LabelPosition.Before,
    value: Int? = null,
    valueVal: Val<Int>? = null,
    setValue: ((Int) -> Unit)? = null,
    min: Int? = null,
    max: Int? = null,
    step: Int? = null,
) : NumberInput<Int>(
    scope,
    hidden,
    disabled,
    label,
    labelVal,
    preferredLabelPosition,
    value,
    valueVal,
    setValue,
    min,
    max,
    step,
) {
    override fun getInputValue(input: HTMLInputElement): Int = input.valueAsNumber.toInt()

    override fun setInputValue(input: HTMLInputElement, value: Int) {
        input.valueAsNumber = value.toDouble()
    }
}
