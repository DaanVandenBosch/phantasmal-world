package world.phantasmal.webui.widgets

import org.w3c.dom.Node
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.list.emptyListCell
import world.phantasmal.observable.cell.mutableCell
import world.phantasmal.observable.cell.nullCell
import world.phantasmal.observable.cell.trueCell
import world.phantasmal.webui.dom.Icon
import world.phantasmal.webui.dom.div

class Select<T : Any>(
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
) : LabelledControl(visible, enabled, tooltip, label, labelCell, preferredLabelPosition) {
    private val buttonText = mutableCell(" ")

    private lateinit var menu: Menu<T>
    private val menuVisible = mutableCell(false)
    private var justOpened = false

    override fun Node.createElement() =
        div {
            className = "pw-select"

            this@Select.className?.let { classList.add(it) }

            // Default to a single space so the inner text part won't be hidden.
            observeNow(selected) { buttonText.value = it?.let(itemToString) ?: " " }

            addWidget(Button(
                enabled = enabled,
                textCell = buttonText,
                iconRight = Icon.TriangleDown,
                onMouseDown = ::onButtonMouseDown,
                onMouseUp = { onButtonMouseUp() },
                onKeyDown = ::onButtonKeyDown,
            )).element.id = labelId
            menu = addWidget(Menu(
                visible = menuVisible,
                enabled = enabled,
                items = items,
                itemToString = itemToString,
                onSelect = ::select,
                onCancel = { menuVisible.value = false },
            ))
        }

    private fun onButtonMouseDown(e: MouseEvent) {
        e.stopPropagation()
        justOpened = !menuVisible.value
        menuVisible.value = true
        selected.value?.let(menu::highlightItem)
    }

    private fun onButtonMouseUp() {
        if (justOpened) {
            menu.focus()
        } else {
            menuVisible.value = false
        }

        justOpened = false
    }

    private fun onButtonKeyDown(e: KeyboardEvent) {
        when (e.key) {
            "ArrowDown" -> {
                if (items.value.isNotEmpty()) {
                    if (selected.value == null) {
                        select(items.value.first())
                    } else {
                        val index = items.value.indexOf(selected.value) + 1

                        if (index >= items.value.size) {
                            select(items.value.first())
                        } else {
                            select(items.value[index])
                        }
                    }
                }
            }

            "ArrowUp" -> {
                if (items.value.isNotEmpty()) {
                    if (selected.value == null) {
                        select(items.value.last())
                    } else {
                        val index = items.value.indexOf(selected.value) - 1

                        if (index < 0) {
                            select(items.value.last())
                        } else {
                            select(items.value[index])
                        }
                    }
                }
            }

            "Enter", " " -> {
                e.preventDefault()
                e.stopPropagation()

                justOpened = !menuVisible.value
                menuVisible.value = true
                selected.value?.let(menu::highlightItem)
                menu.focus()
            }
        }
    }

    private fun select(item: T) {
        menuVisible.value = false
        onSelect(item)
    }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-select {
                    position: relative;
                    display: inline-flex;
                    width: 160px;
                }

                .pw-select .pw-button {
                    flex: 1;
                }

                .pw-select .pw-menu {
                    top: 23px;
                    left: 0;
                    min-width: 100%;
                }
            """.trimIndent())
        }
    }
}
