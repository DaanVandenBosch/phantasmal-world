package world.phantasmal.webui.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.Node
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.observable.value.value
import world.phantasmal.webui.dom.Icon
import world.phantasmal.webui.dom.div

class Select<T : Any>(
    scope: CoroutineScope,
    hidden: Val<Boolean> = falseVal(),
    disabled: Val<Boolean> = falseVal(),
    tooltip: String? = null,
    label: String? = null,
    labelVal: Val<String>? = null,
    preferredLabelPosition: LabelPosition = LabelPosition.Before,
    items: List<T>? = null,
    itemsVal: Val<List<T>>? = null,
    private val itemToString: (T) -> String = Any::toString,
    selected: T? = null,
    selectedVal: Val<T?>? = null,
    private val onSelect: (T) -> Unit = {},
) : LabelledControl(
    scope,
    hidden,
    disabled,
    tooltip,
    label,
    labelVal,
    preferredLabelPosition,
) {
    private val items: Val<List<T>> = itemsVal ?: value(items ?: emptyList())
    private val selected: Val<T?> = selectedVal ?: value(selected)

    private val buttonText = mutableVal(" ")
    private val menuHidden = mutableVal(true)

    private lateinit var menu: Menu<T>
    private var justOpened = false

    override fun Node.createElement() =
        div {
            className = "pw-select"

            // Default to a single space so the inner text part won't be hidden.
            observe(selected) { buttonText.value = it?.let(itemToString) ?: " " }

            addWidget(Button(
                scope,
                disabled = disabled,
                textVal = buttonText,
                iconRight = Icon.TriangleDown,
                onMouseDown = ::onButtonMouseDown,
                onMouseUp = { onButtonMouseUp() },
                onKeyDown = ::onButtonKeyDown,
            ))
            menu = addWidget(Menu(
                scope,
                hidden = menuHidden,
                disabled = disabled,
                itemsVal = items,
                itemToString = itemToString,
                onSelect = ::select,
                onCancel = { menuHidden.value = true },
            ))
        }

    private fun onButtonMouseDown(e: MouseEvent) {
        e.stopPropagation()
        justOpened = menuHidden.value
        menuHidden.value = false
        selected.value?.let(menu::highlightItem)
    }

    private fun onButtonMouseUp() {
        if (justOpened) {
            menu.focus()
        } else {
            menuHidden.value = true
        }

        justOpened = false
    }

    private fun onButtonKeyDown(e: KeyboardEvent) {
        when (e.key) {
            "Enter", " " -> {
                e.preventDefault()
                e.stopPropagation()

                justOpened = menuHidden.value
                menuHidden.value = false
                selected.value?.let(menu::highlightItem)
                menu.focus()
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
        }
    }

    private fun select(item: T) {
        menuHidden.value = true
        buttonText.value = itemToString(item)
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
                    top: 25px;
                    left: 0;
                    min-width: 100%;
                }
            """.trimIndent())
        }
    }
}
