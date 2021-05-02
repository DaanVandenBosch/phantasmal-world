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

class Dropdown<T : Any>(
    visible: Cell<Boolean> = trueCell(),
    enabled: Cell<Boolean> = trueCell(),
    tooltip: Cell<String?> = nullCell(),
    private val text: String? = null,
    private val iconLeft: Icon? = null,
    items: Cell<List<T>>? = null,
    private val itemToString: (T) -> String = Any::toString,
    private val onSelect: (T) -> Unit = {},
) : Control(visible, enabled, tooltip) {
    private val items: Cell<List<T>> = items ?: emptyListCell()

    private val menuVisible = mutableCell(false)

    private lateinit var menu: Menu<T>
    private var justOpened = false

    override fun Node.createElement() =
        div {
            className = "pw-dropdown"

            addWidget(Button(
                enabled = enabled,
                text = text,
                iconLeft = iconLeft,
                iconRight = Icon.TriangleDown,
                onMouseDown = ::onButtonMouseDown,
                onMouseUp = { onButtonMouseUp() },
                onKeyDown = ::onButtonKeyDown,
            ))
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
            "Enter", " " -> {
                e.preventDefault()
                e.stopPropagation()

                justOpened = !menuVisible.value
                menuVisible.value = true
                items.value.firstOrNull()?.let(menu::highlightItem)
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
                .pw-dropdown {
                    position: relative;
                }

                .pw-dropdown > .pw-menu {
                    top: 23px;
                    left: 0;
                    min-width: 100%;
                }
            """.trimIndent())
        }
    }
}
