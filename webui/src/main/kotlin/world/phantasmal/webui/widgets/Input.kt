package world.phantasmal.webui.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.Node
import world.phantasmal.observable.value.Val
import world.phantasmal.webui.dom.input
import world.phantasmal.webui.dom.span

abstract class Input<T>(
    scope: CoroutineScope,
    visible: Val<Boolean>,
    enabled: Val<Boolean>,
    tooltip: Val<String?>,
    label: String?,
    labelVal: Val<String>?,
    preferredLabelPosition: LabelPosition,
    private val className: String,
    private val inputClassName: String,
    private val inputType: String,
    private val value: T?,
    private val valueVal: Val<T>?,
    private val onChange: (T) -> Unit,
    private val maxLength: Int?,
    private val min: Int?,
    private val max: Int?,
    private val step: Int?,
) : LabelledControl(
    scope,
    visible,
    enabled,
    tooltip,
    label,
    labelVal,
    preferredLabelPosition,
) {
    override fun Node.createElement() =
        span {
            classList.add("pw-input", this@Input.className)

            input {
                classList.add("pw-input-inner", inputClassName)
                type = inputType

                observe(this@Input.enabled) { disabled = !it }

                onchange = { onChange(getInputValue(this)) }

                onkeydown = { e ->
                    if (e.key == "Enter") {
                        onChange(getInputValue(this))
                    }
                }

                if (valueVal != null) {
                    observe(valueVal) { setInputValue(this, it) }
                } else if (this@Input.value != null) {
                    setInputValue(this, this@Input.value)
                }

                this@Input.maxLength?.let { maxLength = it }
                this@Input.min?.let { min = it.toString() }
                this@Input.max?.let { max = it.toString() }
                this@Input.step?.let { step = it.toString() }
            }
        }

    protected abstract fun getInputValue(input: HTMLInputElement): T

    protected abstract fun setInputValue(input: HTMLInputElement, value: T)

    companion object {
        init {
            @Suppress("CssUnusedSymbol", "CssUnresolvedCustomProperty")
            // language=css
            style("""
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
            """.trimIndent())
        }
    }
}
