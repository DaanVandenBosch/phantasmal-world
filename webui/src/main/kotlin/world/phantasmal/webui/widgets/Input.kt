package world.phantasmal.webui.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.Node
import world.phantasmal.observable.value.Val
import world.phantasmal.webui.dom.input
import world.phantasmal.webui.dom.span

abstract class Input<T>(
    scope: CoroutineScope,
    styles: List<() -> String>,
    hidden: Val<Boolean>,
    disabled: Val<Boolean>,
    label: String?,
    labelVal: Val<String>?,
    preferredLabelPosition: LabelPosition,
    private val className: String,
    private val inputClassName: String,
    private val inputType: String,
    private val value: T?,
    private val valueVal: Val<T>?,
    private val setValue: ((T) -> Unit)?,
    private val min: Int?,
    private val max: Int?,
    private val step: Int?,
) : LabelledControl(
    scope,
    styles + ::style,
    hidden,
    disabled,
    label,
    labelVal,
    preferredLabelPosition,
) {
    override fun Node.createElement() =
        span(className = "pw-input") {
            classList.add(className)

            input(className = "pw-input-inner", type = inputType) {
                classList.add(inputClassName)

                observe(this@Input.disabled) { disabled = it }

                if (setValue != null) {
                    onchange = { setValue.invoke(getInputValue(this)) }

                    onkeydown = { e ->
                        if (e.key == "Enter") {
                            setValue.invoke(getInputValue(this))
                        }
                    }
                }

                if (valueVal != null) {
                    observe(valueVal) { setInputValue(this, it) }
                } else if (this@Input.value != null) {
                    setInputValue(this, this@Input.value)
                }

                if (this@Input.min != null) {
                    min = this@Input.min.toString()
                }

                if (this@Input.max != null) {
                    max = this@Input.max.toString()
                }

                if (this@Input.step != null) {
                    step = this@Input.step.toString()
                }
            }
        }

    protected abstract fun getInputValue(input: HTMLInputElement): T

    protected abstract fun setInputValue(input: HTMLInputElement, value: T)
}

@Suppress("CssUnusedSymbol", "CssUnresolvedCustomProperty")
// language=css
private fun style() = """
.pw-input {
    display: inline-block;
    box-sizing: border-box;
    height: 24px;
    border: var(--pw-input-border);
}

.pw-input .pw-input-inner {
    box-sizing: border-box;
    width: 100%;
    height: 100%;
    padding: 0 3px;
    border: var(--pw-input-inner-border);
    background-color: var(--pw-input-bg-color);
    color: var(--pw-input-text-color);
    outline: none;
    font-size: 13px;
}

.pw-input:hover {
    border: var(--pw-input-border-hover);
}

.pw-input:focus-within {
    border: var(--pw-input-border-focus);
}

.pw-input.disabled {
    border: var(--pw-input-border-disabled);
}

.pw-input.disabled .pw-input-inner {
    color: var(--pw-input-text-color-disabled);
    background-color: var(--pw-input-bg-color-disabled);
}
"""
