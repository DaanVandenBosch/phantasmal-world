package world.phantasmal.webui.widgets

import org.w3c.dom.Node
import world.phantasmal.observable.value.*
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.dom.textarea

class TextArea(
    visible: Val<Boolean> = trueVal(),
    enabled: Val<Boolean> = trueVal(),
    tooltip: Val<String?> = nullVal(),
    label: String? = null,
    labelVal: Val<String>? = null,
    preferredLabelPosition: LabelPosition = LabelPosition.Before,
    private val value: Val<String> = emptyStringVal(),
    private val onChange: ((String) -> Unit)? = null,
    private val maxLength: Int? = null,
    private val fontFamily: String? = null,
    private val rows: Int? = null,
    private val cols: Int? = null,
) : LabelledControl(
    visible,
    enabled,
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

                observe(this@TextArea.enabled) { disabled = !it }

                if (onChange != null) {
                    onchange = { onChange.invoke(value) }
                }

                observe(this@TextArea.value) { value = it }

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
