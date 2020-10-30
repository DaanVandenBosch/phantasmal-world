package world.phantasmal.webui.widgets

import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal
import world.phantasmal.observable.value.value
import world.phantasmal.webui.dom.disposableListener
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.newJsObject

class Menu<T : Any>(
    scope: CoroutineScope,
    hidden: Val<Boolean> = falseVal(),
    disabled: Val<Boolean> = falseVal(),
    tooltip: String? = null,
    items: List<T>? = null,
    itemsVal: Val<List<T>>? = null,
    private val itemToString: (T) -> String = Any::toString,
    private val onSelect: (T) -> Unit = {},
    private val onCancel: () -> Unit = {},
) : Widget(
    scope,
    hidden,
    disabled,
    tooltip,
) {
    private val items: Val<List<T>> = itemsVal ?: value(items ?: emptyList())
    private lateinit var innerElement: HTMLElement
    private var highlightedIndex: Int? = null
    private var highlightedElement: Element? = null
    private var previouslyFocusedElement: Element? = null

    override fun Node.createElement() =
        div {
            className = "pw-menu"
            tabIndex = -1
            onmouseup = ::onMouseUp
            onkeydown = ::onKeyDown
            onblur = { onBlur() }

            innerElement = div {
                className = "pw-menu-inner"
                onmouseover = ::innerMouseOver

                bindChildrenTo(items) { item, index ->
                    div {
                        dataset["index"] = index.toString()
                        textContent = itemToString(item)
                    }
                }
            }

            observe(this@Menu.hidden) {
                if (it) {
                    document.removeEventListener("mousedown", ::onDocumentMouseDown)
                    clearHighlightItem()

                    (previouslyFocusedElement as HTMLElement?)?.focus()
                } else {
                    document.addEventListener("mousedown", ::onDocumentMouseDown)
                }
            }

            observe(disabled) {
                if (it) {
                    clearHighlightItem()
                }
            }

            disposableListener(document, "keydown", ::onDocumentKeyDown)
        }

    override fun internalDispose() {
        document.removeEventListener("mousedown", ::onDocumentMouseDown)
        super.internalDispose()
    }

    override fun focus() {
        previouslyFocusedElement = document.activeElement
        super.focus()
    }

    fun highlightItem(item: T) {
        val idx = items.value.indexOf(item)

        if (idx != -1) {
            highlightItemAt(idx)
        }
    }

    private fun onMouseUp(e: MouseEvent) {
        val target = e.target

        if (target !is HTMLElement) return

        target.dataset["index"]?.toIntOrNull()?.let(::selectItem)
    }

    private fun onKeyDown(e: KeyboardEvent) {
        when (e.key) {
            "ArrowDown" -> {
                e.preventDefault()
                highlightItemAt(
                    when (val idx = highlightedIndex) {
                        null, items.value.lastIndex -> 0
                        else -> idx + 1
                    }
                )
            }

            "ArrowUp" -> {
                e.preventDefault()
                highlightItemAt(
                    when (val idx = highlightedIndex) {
                        null, 0 -> items.value.lastIndex
                        else -> idx - 1
                    }
                )
            }

            "Enter", " " -> {
                e.preventDefault()
                e.stopPropagation()
                highlightedIndex?.let(::selectItem)
            }
        }
    }

    private fun onBlur() {
        onCancel()
    }

    private fun innerMouseOver(e: MouseEvent) {
        val target = e.target

        if (target is HTMLElement) {
            target.dataset["index"]?.toIntOrNull()?.let(::highlightItemAt)
        }
    }

    private fun onDocumentMouseDown(e: Event) {
        val target = e.target

        if (target !is Node || !element.contains(target)) {
            onCancel()
        }
    }

    private fun onDocumentKeyDown(e: KeyboardEvent) {
        if (e.key === "Escape") {
            onCancel()
        }
    }

    private fun clearHighlightItem() {
        highlightedElement?.classList?.remove("pw-menu-highlighted")
        highlightedIndex = null
        highlightedElement = null
    }

    private fun highlightItemAt(index: Int) {
        highlightedElement?.classList?.remove("pw-menu-highlighted")

        if (disabled.value) return

        highlightedElement = innerElement.children.item(index)

        highlightedElement?.let {
            highlightedIndex = index
            it.classList.add("pw-menu-highlighted")
            it.scrollIntoView(newJsObject { block = "nearest" })
        }
    }

    private fun selectItem(index: Int) {
        if (disabled.value) return

        items.value.getOrNull(index)?.let(onSelect)
    }

    companion object {
        init {
            @Suppress("CssUnusedSymbol", "CssUnresolvedCustomProperty")
            // language=css
            style("""
                .pw-menu {
                    z-index: 1000;
                    position: absolute;
                    box-sizing: border-box;
                    outline: none;
                    border: var(--pw-control-border);
                    --scrollbar-color: hsl(0, 0%, 18%);
                    --scrollbar-thumb-color: hsl(0, 0%, 22%);
                }

                .pw-menu > .pw-menu-inner {
                    overflow: auto;
                    background-color: var(--pw-control-bg-color);
                    max-height: 500px;
                    border: var(--pw-control-inner-border);
                }

                .pw-menu > .pw-menu-inner > * {
                    padding: 4px 8px;
                    white-space: nowrap;
                }

                .pw-menu > .pw-menu-inner > .pw-menu-highlighted {
                    background-color: var(--pw-control-bg-color-hover);
                    color: var(--pw-control-text-color-hover);
                }
            """.trimIndent())
        }
    }
}
