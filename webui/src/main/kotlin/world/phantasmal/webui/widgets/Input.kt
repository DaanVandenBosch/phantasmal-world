package world.phantasmal.webui.widgets

import org.w3c.dom.HTMLInputElement
import org.w3c.dom.Node
import world.phantasmal.observable.cell.Cell
import world.phantasmal.webui.dom.input
import world.phantasmal.webui.dom.span

abstract class Input<T>(
    visible: Cell<Boolean>,
    enabled: Cell<Boolean>,
    tooltip: Cell<String?>,
    label: String?,
    labelCell: Cell<String>?,
    preferredLabelPosition: LabelPosition,
    private val className: String,
    private val value: Cell<T>,
    private val onChange: (T) -> Unit,
) : LabelledControl(
    visible,
    enabled,
    tooltip,
    label,
    labelCell,
    preferredLabelPosition,
) {
    private var settingValue = false

    override fun Node.createElement() =
        span {
            classList.add("pw-input", this@Input.className)

            input {
                id = labelId
                classList.add("pw-input-inner")

                observeNow(this@Input.enabled) { disabled = !it }

                onchange = { callOnChange(this) }

                interceptInputElement(this)

                observeNow(this@Input.value) {
                    setInputValue(this, it)
                }
            }
        }

    /**
     * Called during [createElement].
     */
    protected open fun interceptInputElement(input: HTMLInputElement) {}

    protected abstract fun getInputValue(input: HTMLInputElement): T

    protected abstract fun setInputValue(input: HTMLInputElement, value: T)

    private fun callOnChange(input: HTMLInputElement) {
        val v = getInputValue(input)

        if (!valuesEqual(v, this@Input.value.value)) {
            onChange(v)
        }
    }

    protected open fun valuesEqual(a: T, b: T): Boolean =
        a == b

    companion object {
        init {
            @Suppress("CssUnusedSymbol", "CssUnresolvedCustomProperty")
            // language=css
            style("""
                .pw-input {
                    display: inline-block;
                    box-sizing: border-box;
                    height: 22px;
                    border: var(--pw-input-border);
                }

                .pw-input-inner {
                    box-sizing: border-box;
                    width: 100%;
                    height: 100%;
                    padding: 0 2px;
                    border: var(--pw-input-inner-border);
                    background-color: var(--pw-input-bg-color);
                    color: var(--pw-input-text-color);
                    outline: none;
                    font-size: 12px;
                }

                .pw-input:hover {
                    border: var(--pw-input-border-hover);
                }

                .pw-input:focus-within {
                    border: var(--pw-input-border-focus);
                }

                .pw-input.pw-disabled {
                    border: var(--pw-input-border-disabled);
                }

                .pw-input.pw-disabled > .pw-input-inner {
                    color: var(--pw-input-text-color-disabled);
                    background-color: var(--pw-input-bg-color-disabled);
                }
            """.trimIndent())
        }
    }
}
