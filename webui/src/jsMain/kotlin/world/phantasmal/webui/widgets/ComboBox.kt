package world.phantasmal.webui.widgets

import org.w3c.dom.HTMLInputElement
import org.w3c.dom.Node
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import world.phantasmal.cell.Cell
import world.phantasmal.cell.list.emptyListCell
import world.phantasmal.cell.mutableCell
import world.phantasmal.cell.not
import world.phantasmal.cell.nullCell
import world.phantasmal.cell.trueCell
import world.phantasmal.webui.dom.*

class ComboBox<T : Any>(
    visible: Cell<Boolean> = trueCell(),
    enabled: Cell<Boolean> = trueCell(),
    tooltip: Cell<String?> = nullCell(),
    private val className: String? = null,
    label: String? = null,
    labelCell: Cell<String>? = null,
    preferredLabelPosition: LabelPosition = LabelPosition.Before,
    private val items: Cell<List<T>> = emptyListCell(),
    private val itemToString: (T) -> String = Any::toString,
    private val selected: Cell<T?> = nullCell(),
    private val onSelect: (T) -> Unit = {},
    private val placeholderText: String? = null,
    private val filter: (String) -> Unit = {},
) : LabelledControl(visible, enabled, tooltip, label, labelCell, preferredLabelPosition) {
    private lateinit var input: HTMLInputElement
    private var prevInputValue = ""

    private lateinit var menu: Menu<T>
    private val menuVisible = mutableCell(false)

    override fun Node.createElement() =
        div {
            className = "pw-combobox"

            this@ComboBox.className?.let { classList.add(it) }

            span {
                className = "pw-combobox-inner"

                input = input {
                    id = labelId
                    placeholderText?.let { placeholder = it }
                    hidden(!visible)
                    observeNow(enabled) { disabled = !it }
                    observeNow(selected) { value = it?.let(itemToString) ?: "" }

                    onmousedown = ::onInputMouseDown
                    onkeydown = ::onInputKeyDown
                    onkeyup = { onInputKeyUp() }
                    onblur = { onInputBlur() }
                }
                span {
                    className = "pw-combobox-button"

                    onmousedown = ::onButtonMouseDown

                    icon(Icon.TriangleDown).hidden(menuVisible)
                    icon(Icon.TriangleUp).hidden(!menuVisible)
                }
            }

            menu = addWidget(Menu(
                visible = menuVisible,
                enabled = enabled,
                items = items,
                itemToString = itemToString,
                onSelect = ::select,
                onCancel = { menuVisible.value = false },
            ))

            // Avoid input blur event from triggering when clicking menu as this would close the
            // menu before a mouse up can be registered.
            menu.element.onmousedown = { it.preventDefault() }
        }

    private fun onInputMouseDown(e: MouseEvent) {
        e.stopPropagation()
        menuVisible.value = true
        selected.value?.let(menu::highlightItem)
    }

    private fun onInputKeyDown(e: KeyboardEvent) {
        when (e.key) {
            "ArrowDown" -> {
                menuVisible.value = true
                menu.highlightNext()
            }

            "ArrowUp" -> {
                menuVisible.value = true
                menu.highlightPrev()
            }

            "Enter" -> {
                menu.selectHighlighted()
            }
        }
    }

    private fun onInputKeyUp() {
        val inputValue = input.value

        if (inputValue != prevInputValue) {
            filter(inputValue)

            if (menuVisible.value || inputValue.isNotEmpty()) {
                menuVisible.value = true
                menu.highlightNext()
            }

            prevInputValue = inputValue
        }
    }

    private fun onInputBlur() {
        menuVisible.value = false
    }

    private fun onButtonMouseDown(e: MouseEvent) {
        // Call preventDefault so we don't trigger a blur event on the input element, which would
        // cause us to close the menu.
        e.preventDefault()
        // Call stopPropagation to stop menu from immediately closing after opening.
        e.stopPropagation()
        // Focus the input if it wasn't already focused.
        input.focus()
        menuVisible.value = !menuVisible.value
    }

    private fun select(item: T) {
        menuVisible.value = false
        input.value = selected.value?.let(itemToString) ?: ""
        input.focus()
        onSelect(item)
    }

    companion object {
        init {
            @Suppress("CssUnusedSymbol", "CssUnresolvedCustomProperty")
            // language=css
            style("""
                .pw-combobox {
                    display: inline-block;
                    box-sizing: border-box;
                    position: relative;
                    height: 22px;
                    border: var(--pw-input-border);
                }

                .pw-combobox:hover {
                    border: var(--pw-input-border-hover);
                }

                .pw-combobox:focus-within {
                    border: var(--pw-input-border-focus);
                }

                .pw-combobox.pw-disabled {
                    border: var(--pw-input-border-disabled);
                }

                .pw-combobox-inner {
                    box-sizing: border-box;
                    display: flex;
                    align-items: center;
                    width: 100%;
                    height: 100%;
                    padding: 0 2px;
                    border: var(--pw-input-inner-border);
                    background-color: var(--pw-input-bg-color);
                    color: var(--pw-input-text-color);
                }

                .pw-combobox.pw-disabled > .pw-combobox-inner {
                    background-color: var(--pw-input-bg-color-disabled);
                }

                .pw-combobox-inner > input {
                    flex: 1;
                    box-sizing: border-box;
                    padding: 0;
                    border: none;
                    margin: 0;
                    color: var(--pw-input-text-color);
                    background-color: transparent;
                    outline: none;
                    font-size: 12px;
                }

                .pw-combobox.pw-disabled > .pw-combobox-inner > input {
                    color: var(--pw-input-text-color-disabled);
                }

                .pw-combobox > .pw-menu {
                    top: 21px;
                    left: -2px;
                    min-width: calc(100% + 4px);
                }

                .pw-combobox-button {
                    padding: 0 2px;
                }
            """.trimIndent())
        }
    }
}
