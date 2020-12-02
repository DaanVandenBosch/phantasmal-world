package world.phantasmal.webui.widgets

import org.w3c.dom.HTMLInputElement
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.nullVal
import world.phantasmal.observable.value.trueVal
import world.phantasmal.observable.value.value
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round

class DoubleInput(
    visible: Val<Boolean> = trueVal(),
    enabled: Val<Boolean> = trueVal(),
    tooltip: Val<String?> = nullVal(),
    label: String? = null,
    labelVal: Val<String>? = null,
    preferredLabelPosition: LabelPosition = LabelPosition.Before,
    value: Val<Double> = value(0.0),
    onChange: (Double) -> Unit = {},
    roundTo: Int = 2,
) : NumberInput<Double>(
    visible,
    enabled,
    tooltip,
    label,
    labelVal,
    preferredLabelPosition,
    value,
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

    override fun valuesEqual(a: Double, b: Double): Boolean =
        abs(a - b) * roundingFactor < 1.0
}
