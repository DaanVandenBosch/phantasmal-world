package world.phantasmal.webui.widgets

import org.w3c.dom.HTMLInputElement
import world.phantasmal.cell.Cell
import world.phantasmal.cell.cell
import world.phantasmal.cell.nullCell
import world.phantasmal.cell.trueCell

class IntInput(
    visible: Cell<Boolean> = trueCell(),
    enabled: Cell<Boolean> = trueCell(),
    tooltip: Cell<String?> = nullCell(),
    label: String? = null,
    labelCell: Cell<String>? = null,
    preferredLabelPosition: LabelPosition = LabelPosition.Before,
    value: Cell<Int> = cell(0),
    onChange: (Int) -> Unit = {},
    min: Int? = null,
    max: Int? = null,
    step: Int? = null,
) : NumberInput<Int>(
    visible,
    enabled,
    tooltip,
    label,
    labelCell,
    preferredLabelPosition,
    value,
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
