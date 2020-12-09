package world.phantasmal.webui.widgets

import org.w3c.dom.HTMLInputElement
import world.phantasmal.observable.value.Val

abstract class NumberInput<T : Number>(
    visible: Val<Boolean>,
    enabled: Val<Boolean>,
    tooltip: Val<String?>,
    label: String?,
    labelVal: Val<String>?,
    preferredLabelPosition: LabelPosition,
    value: Val<T>,
    onChange: (T) -> Unit,
    private val min: Int?,
    private val max: Int?,
    private val step: Int?,
) : Input<T>(
    visible,
    enabled,
    tooltip,
    label,
    labelVal,
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
                    width: 54px;
                }

                .pw-number-input-inner {
                    padding-right: 1px;
                }
            """.trimIndent())
        }
    }
}
