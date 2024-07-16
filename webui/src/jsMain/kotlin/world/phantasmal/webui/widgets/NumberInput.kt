package world.phantasmal.webui.widgets

import org.w3c.dom.HTMLInputElement
import world.phantasmal.cell.Cell

abstract class NumberInput<T : Number>(
    visible: Cell<Boolean>,
    enabled: Cell<Boolean>,
    tooltip: Cell<String?>,
    label: String?,
    labelCell: Cell<String>?,
    preferredLabelPosition: LabelPosition,
    value: Cell<T>,
    onChange: (T) -> Unit,
    private val min: Int?,
    private val max: Int?,
    private val step: Int?,
) : Input<T>(
    visible,
    enabled,
    tooltip,
    label,
    labelCell,
    preferredLabelPosition,
    className = "pw-number-input",
    value,
    onChange,
) {
    override fun interceptInputElement(input: HTMLInputElement) {
        super.interceptInputElement(input)

        input.type = "number"
        input.classList.add("pw-number-input-inner")
        min?.let { input.min = it.toString() }
        max?.let { input.max = it.toString() }
        input.step = step?.toString() ?: "any"
    }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-number-input {
                    width: 60px;
                }

                .pw-input-inner.pw-number-input-inner {
                    padding-right: 0;
                }
            """.trimIndent())
        }
    }
}
