package world.phantasmal.webui.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.HTMLInputElement
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal
import kotlin.math.pow
import kotlin.math.round

class DoubleInput(
    scope: CoroutineScope,
    hidden: Val<Boolean> = falseVal(),
    disabled: Val<Boolean> = falseVal(),
    tooltip: String? = null,
    label: String? = null,
    labelVal: Val<String>? = null,
    preferredLabelPosition: LabelPosition = LabelPosition.Before,
    value: Double? = null,
    valueVal: Val<Double>? = null,
    onChange: (Double) -> Unit = {},
    roundTo: Int = 2,
) : NumberInput<Double>(
    scope,
    hidden,
    disabled,
    tooltip,
    label,
    labelVal,
    preferredLabelPosition,
    value,
    valueVal,
    onChange,
    min = null,
    max = null,
    step = null,
) {
    private val roundingFactor: Double =
        if (roundTo < 0) 1.0 else (10.0).pow(roundTo)

    override fun getInputValue(input: HTMLInputElement): Double = input.valueAsNumber

    override fun setInputValue(input: HTMLInputElement, value: Double) {
        input.valueAsNumber = round(value * roundingFactor) / roundingFactor
    }
}
