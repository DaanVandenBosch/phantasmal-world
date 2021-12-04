package world.phantasmal.webui.widgets

import org.w3c.dom.Node
import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.emptyStringCell
import world.phantasmal.observable.cell.nullCell
import world.phantasmal.observable.cell.trueCell
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.dom.textarea

class TextArea(
    visible: Cell<Boolean> = trueCell(),
    enabled: Cell<Boolean> = trueCell(),
    tooltip: Cell<String?> = nullCell(),
    label: String? = null,
    labelCell: Cell<String>? = null,
    preferredLabelPosition: LabelPosition = LabelPosition.Before,
    private val value: Cell<String> = emptyStringCell(),
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
    labelCell,
    preferredLabelPosition,
) {
    override fun Node.createElement() =
        div {
            className = "pw-text-area"

            textarea {
                id = labelId
                className = "pw-text-area-inner"

                observeNow(this@TextArea.enabled) { disabled = !it }

                if (onChange != null) {
                    onchange = { onChange.invoke(value) }
                }

                observeNow(this@TextArea.value) { value = it }

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
                    font-size: 12px;
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
