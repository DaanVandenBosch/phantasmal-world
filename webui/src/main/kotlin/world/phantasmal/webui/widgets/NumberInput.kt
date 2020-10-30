package world.phantasmal.webui.widgets

import kotlinx.coroutines.CoroutineScope
import world.phantasmal.observable.value.Val

abstract class NumberInput<T : Number>(
    scope: CoroutineScope,
    hidden: Val<Boolean>,
    disabled: Val<Boolean>,
    tooltip: String?,
    label: String?,
    labelVal: Val<String>?,
    preferredLabelPosition: LabelPosition,
    value: T?,
    valueVal: Val<T>?,
    onChange: (T) -> Unit,
    min: Int?,
    max: Int?,
    step: Int?,
) : Input<T>(
    scope,
    hidden,
    disabled,
    tooltip,
    label,
    labelVal,
    preferredLabelPosition,
    className = "pw-number-input",
    inputClassName = "pw-number-input-inner",
    inputType = "number",
    value,
    valueVal,
    onChange,
    maxLength = null,
    min,
    max,
    step,
) {
    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-number-input {
                    width: 54px;
                }

                .pw-number-input .pw-number-input-inner {
                    padding-right: 1px;
                }
            """.trimIndent())
        }
    }
}
