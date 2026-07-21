package world.phantasmal.webui.widgets

import org.w3c.dom.HTMLInputElement
import world.phantasmal.cell.Cell
import world.phantasmal.cell.emptyStringCell
import world.phantasmal.cell.nullCell
import world.phantasmal.cell.trueCell

class TextInput(
    visible: Cell<Boolean> = trueCell(),
    enabled: Cell<Boolean> = trueCell(),
    tooltip: Cell<String?> = nullCell(),
    label: String? = null,
    labelCell: Cell<String>? = null,
    preferredLabelPosition: LabelPosition = LabelPosition.Before,
    value: Cell<String> = emptyStringCell(),
    onChange: (String) -> Unit = {},
    private val maxLength: Int? = null,
    private val placeholder: String? = null,
    /** Additional class name applied to the inner `<input>` element for custom styling. */
    private val extraClassName: String? = null,
) : Input<String>(
    visible,
    enabled,
    tooltip,
    label,
    labelCell,
    preferredLabelPosition,
    className = "pw-text-input",
    value,
    onChange,
) {
    override fun interceptInputElement(input: HTMLInputElement) {
        super.interceptInputElement(input)

        input.type = "text"
        maxLength?.let { input.maxLength = it }
        placeholder?.let { input.placeholder = it }
        extraClassName?.let { input.classList.add(it) }
    }

    override fun getInputValue(input: HTMLInputElement): String = input.value

    override fun setInputValue(input: HTMLInputElement, value: String) {
        input.value = value
    }
}
