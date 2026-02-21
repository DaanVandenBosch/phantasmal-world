package world.phantasmal.webui.widgets

import kotlinx.browser.document
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.cell.Cell
import world.phantasmal.cell.list.emptyListCell
import world.phantasmal.cell.nullCell
import world.phantasmal.cell.trueCell
import world.phantasmal.webui.dom.disposableListener
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.obj

class Menu<T : Any>(
    visible: Cell<Boolean> = trueCell(),
    enabled: Cell<Boolean> = trueCell(),
    tooltip: Cell<String?> = nullCell(),
    private val items: Cell<List<T>> = emptyListCell(),
    private val itemToString: (T) -> String = Any::toString,
    private val onSelect: (T) -> Unit = {},
    private val onCancel: () -> Unit = {},
) : Widget(
    visible,
    enabled,
    tooltip,
) {
    private lateinit var innerElement: HTMLElement
    private var highlightedIndex: Int? = null
    private var highlightedElement: Element? = null
    private var previouslyFocusedElement: Element? = null
    private var onDocumentMouseDownListener: Disposable? = null

    override fun Node.createElement() =
        div {
            className = "pw-menu"
            tabIndex = -1
            onmouseup = ::onMouseUp
            onkeydown = ::onKeyDown
            onblur = { onBlur() }

            innerElement = div {
                className = "pw-menu-inner"
                onmouseover = ::onInnerMouseOver

                bindChildrenTo(items) { item, index ->
                    div {
                        dataset["index"] = index.toString()
                        textContent = itemToString(item)
                    }
                }
            }

            observeNow(this@Menu.visible) {
                if (it) {
                    onDocumentMouseDownListener =
                        document.disposableListener("mousedown", ::onDocumentMouseDown)
                } else {
                    onDocumentMouseDownListener?.dispose()
                    onDocumentMouseDownListener = null
                    clearHighlightItem()

                    (previouslyFocusedElement as HTMLElement?)?.focus()
                }
            }

            observeNow(enabled) {
                if (!it) {
                    clearHighlightItem()
                }
            }

            observeNow(items) {
                clearHighlightItem()
            }

            addDisposable(document.disposableListener("keydown", ::onDocumentKeyDown))
        }

    override fun dispose() {
        onDocumentMouseDownListener?.dispose()
        super.dispose()
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

    fun highlightNext() {
        highlightItemAt(
            when (val idx = highlightedIndex) {
                null, items.value.lastIndex -> 0
                else -> idx + 1
            }
        )
    }

    fun highlightPrev() {
        highlightItemAt(
            when (val idx = highlightedIndex) {
                null, 0 -> items.value.lastIndex
                else -> idx - 1
            }
        )
    }

    fun selectHighlighted() {
        highlightedIndex?.let(::selectItem)
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
                highlightNext()
            }

            "ArrowUp" -> {
                e.preventDefault()
                highlightPrev()
            }

            "Enter", " " -> {
                e.preventDefault()
                e.stopPropagation()
                selectHighlighted()
            }
        }
    }

    private fun onBlur() {
        onCancel()
    }

    private fun onInnerMouseOver(e: MouseEvent) {
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

        if (!enabled.value) return

        highlightedElement = innerElement.children.item(index)

        highlightedElement?.let {
            highlightedIndex = index
            it.classList.add("pw-menu-highlighted")
            it.scrollIntoView(obj { block = "nearest" })
        }
    }

    private fun selectItem(index: Int) {
        if (!enabled.value) return

        items.value.getOrNull(index)?.let(onSelect)
    }

    companion object {
        init {
            @Suppress("CssUnusedSymbol", "CssUnresolvedCustomProperty")
            // language=css
            style("""
                .pw-menu {
                    z-index: 1001;
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
                    padding: 3px 6px;
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
