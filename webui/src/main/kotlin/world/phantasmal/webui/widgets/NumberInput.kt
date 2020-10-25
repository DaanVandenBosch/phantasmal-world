package world.phantasmal.webui.widgets

import kotlinx.coroutines.CoroutineScope
import world.phantasmal.observable.value.Val

abstract class NumberInput<T : Number>(
    scope: CoroutineScope,
    hidden: Val<Boolean>,
    disabled: Val<Boolean>,
    label: String?,
    labelVal: Val<String>?,
    preferredLabelPosition: LabelPosition,
    value: T?,
    valueVal: Val<T>?,
    setValue: ((T) -> Unit)?,
    min: Int?,
    max: Int?,
    step: Int?,
) : Input<T>(
    scope,
    listOf(::style),
    hidden,
    disabled,
    label,
    labelVal,
    preferredLabelPosition,
    className = "pw-number-input",
    inputClassName = "pw-number-input-inner",
    inputType = "number",
    value,
    valueVal,
    setValue,
    min,
    max,
    step,
)

@Suppress("CssUnusedSymbol")
// language=css
private fun style() = """
.pw-number-input {
    width: 54px;
}

.pw-number-input .pw-number-input-inner {
    padding-right: 1px;
}
"""
