package world.phantasmal.webui.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.Node
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.dom.textarea

class TextArea(
    scope: CoroutineScope,
    hidden: Val<Boolean> = falseVal(),
    disabled: Val<Boolean> = falseVal(),
    tooltip: String? = null,
    label: String? = null,
    labelVal: Val<String>? = null,
    preferredLabelPosition: LabelPosition = LabelPosition.Before,
    private val value: String? = null,
    private val valueVal: Val<String>? = null,
    private val setValue: ((String) -> Unit)? = null,
    private val maxLength: Int? = null,
    private val fontFamily: String? = null,
    private val rows: Int? = null,
    private val cols: Int? = null,
) : LabelledControl(
    scope,
    hidden,
    disabled,
    tooltip,
    label,
    labelVal,
    preferredLabelPosition,
) {
    override fun Node.createElement() =
        div {
            className = "pw-text-area"

            textarea {
                className = "pw-text-area-inner"

                observe(this@TextArea.disabled) { disabled = it }

                if (setValue != null) {
                    onchange = { setValue.invoke(value) }
                }

                if (valueVal != null) {
                    observe(valueVal) { value = it }
                } else if (this@TextArea.value != null) {
                    value = this@TextArea.value
                }

                this@TextArea.maxLength?.let { maxLength = it }
                fontFamily?.let { style.fontFamily = it }
                this@TextArea.rows?.let { rows = it }
                this@TextArea.cols?.let { cols = it }
            }
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol", "CssUnresolvedCustomProperty")
            // language=css
            style("""
                .pw-text-area {
                    box-sizing: border-box;
                    display: inline-block;
                    border: var(--pw-input-border);
                }

                .pw-text-area .pw-text-area-inner {
                    box-sizing: border-box;
                    vertical-align: top;
                    padding: 3px;
                    border: var(--pw-input-inner-border);
                    margin: 0;
                    background-color: var(--pw-input-bg-color);
                    color: var(--pw-input-text-color);
                    outline: none;
                    font-size: 13px;
                }

                .pw-text-area:hover {
                    border: var(--pw-input-border-hover);
                }

                .pw-text-area:focus-within {
                    border: var(--pw-input-border-focus);
                }

                .pw-text-area.disabled {
                    border: var(--pw-input-border-disabled);
                }

                .pw-text-area.disabled .pw-text-area-inner {
                    color: var(--pw-input-text-color-disabled);
                    background-color: var(--pw-input-bg-color-disabled);
                }
            """.trimIndent())
        }
    }
}
