package world.phantasmal.webui.widgets

import org.w3c.dom.HTMLInputElement
import world.phantasmal.cell.Cell
import world.phantasmal.cell.cell
import world.phantasmal.cell.nullCell
import world.phantasmal.cell.trueCell
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round

class DoubleInput(
    visible: Cell<Boolean> = trueCell(),
    enabled: Cell<Boolean> = trueCell(),
    tooltip: Cell<String?> = nullCell(),
    label: String? = null,
    labelCell: Cell<String>? = null,
    preferredLabelPosition: LabelPosition = LabelPosition.Before,
    value: Cell<Double> = cell(0.0),
    onChange: (Double) -> Unit = {},
    roundTo: Int = 2,
    /**
     * Optional paste interceptor. Receives the raw clipboard text and should return `true` to
     * suppress the browser's default single-value paste — useful when the pasted text is a tuple
     * the caller wants to distribute across multiple inputs.
     */
    private val onPaste: ((String) -> Boolean)? = null,
) : NumberInput<Double>(
    visible,
    enabled,
    tooltip,
    label,
    labelCell,
    preferredLabelPosition,
    value,
    onChange,
    min = null,
    max = null,
    step = null,
) {
    private val roundingFactor: Double =
        if (roundTo < 0) 1.0 else (10.0).pow(roundTo)

    override fun interceptInputElement(input: HTMLInputElement) {
        super.interceptInputElement(input)

        if (onPaste != null) {
            input.onpaste = { e ->
                val text = e.clipboardData?.getData("text") ?: ""
                if (onPaste.invoke(text)) {
                    e.preventDefault()
                }
                null
            }
        }
    }

    override fun getInputValue(input: HTMLInputElement): Double = input.valueAsNumber

    override fun setInputValue(input: HTMLInputElement, value: Double) {
        input.valueAsNumber = round(value * roundingFactor) / roundingFactor
    }

    override fun valuesEqual(a: Double, b: Double): Boolean =
        abs(a - b) * roundingFactor < 1.0
}
