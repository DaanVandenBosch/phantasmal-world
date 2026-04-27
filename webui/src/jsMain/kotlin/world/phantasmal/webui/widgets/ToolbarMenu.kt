package world.phantasmal.webui.widgets

import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import world.phantasmal.cell.Cell
import world.phantasmal.cell.mutableCell
import world.phantasmal.cell.nullCell
import world.phantasmal.cell.trueCell
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.webui.dom.Icon
import world.phantasmal.webui.dom.disposableListener
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.dom.icon
import world.phantasmal.webui.dom.span

/**
 * Represents a menu item in a toolbar menu.
 */
sealed class MenuItem {
    /** A regular clickable menu item. */
    data class Action(
        val label: String,
        val shortcut: String? = null,
        val enabled: Cell<Boolean> = trueCell(),
        val onAction: () -> Unit,
    ) : MenuItem()

    /** A checkbox menu item that toggles a boolean value. */
    data class Check(
        val label: String,
        val checked: Cell<Boolean>,
        val tooltip: String? = null,
        val onChange: (Boolean) -> Unit,
    ) : MenuItem()

    /** A submenu that expands to show more items. */
    data class SubMenu(
        val label: String,
        val items: List<MenuItem>,
        val visible: Cell<Boolean> = trueCell(),
    ) : MenuItem()

    /** A visual separator line. */
    data object Separator : MenuItem()
}

/**
 * A dropdown menu button for toolbars that supports various menu item types
 * including actions, checkboxes, submenus, and separators.
 */
class ToolbarMenu(
    visible: Cell<Boolean> = trueCell(),
    enabled: Cell<Boolean> = trueCell(),
    tooltip: Cell<String?> = nullCell(),
    private val text: String,
    private val items: List<MenuItem>,
) : Control(visible, enabled, tooltip) {

    private val menuVisible = mutableCell(false)
    private var justOpened = false
    private lateinit var menuElement: HTMLElement
    private var onDocumentMouseDownListener: Disposable? = null
    private var highlightedIndex: Int? = null
    private var highlightedElement: HTMLElement? = null
    private var activeSubMenu: HTMLElement? = null

    override fun Node.createElement() =
        div {
            className = "pw-toolbar-menu"

            addWidget(Button(
                enabled = enabled,
                text = text,
                iconRight = Icon.TriangleDown,
                onMouseDown = ::onButtonMouseDown,
                onMouseUp = { onButtonMouseUp() },
                onKeyDown = ::onButtonKeyDown,
            ))

            menuElement = div {
                className = "pw-toolbar-menu-popup"
                tabIndex = -1
                onmouseup = ::onMenuMouseUp
                onkeydown = ::onMenuKeyDown
                onblur = { onMenuBlur() }

                renderMenuItems(items)
            }

            observeNow(menuVisible) { visible ->
                menuElement.style.display = if (visible) "block" else "none"
                if (visible) {
                    onDocumentMouseDownListener =
                        document.disposableListener("mousedown", ::onDocumentMouseDown)
                } else {
                    onDocumentMouseDownListener?.dispose()
                    onDocumentMouseDownListener = null
                    clearHighlight()
                    hideAllSubMenus()
                }
            }
        }

    override fun dispose() {
        onDocumentMouseDownListener?.dispose()
        super.dispose()
    }

    private fun Node.renderMenuItems(items: List<MenuItem>) {
        items.forEachIndexed { index, item ->
            when (item) {
                is MenuItem.Action -> div {
                    className = "pw-toolbar-menu-item"
                    dataset.asDynamic().index = index.toString()

                    span {
                        className = "pw-toolbar-menu-item-label"
                        textContent = item.label
                    }
                    if (item.shortcut != null) {
                        span {
                            className = "pw-toolbar-menu-item-shortcut"
                            textContent = item.shortcut
                        }
                    }

                    observeNow(item.enabled) { enabled ->
                        if (enabled) {
                            classList.remove("pw-toolbar-menu-item-disabled")
                        } else {
                            classList.add("pw-toolbar-menu-item-disabled")
                        }
                    }

                    addEventListener("mouseup", { e ->
                        e.stopPropagation()
                        if (item.enabled.value) {
                            menuVisible.value = false
                            item.onAction()
                        }
                    })

                    onmouseover = { highlightItemAt(index) }
                }

                is MenuItem.Check -> div {
                    className = "pw-toolbar-menu-item pw-toolbar-menu-item-check"
                    dataset.asDynamic().index = index.toString()
                    if (item.tooltip != null) {
                        title = item.tooltip
                    }

                    span {
                        className = "pw-toolbar-menu-item-check-icon"
                        observeNow(item.checked) { checked ->
                            textContent = if (checked) "✓" else ""
                        }
                    }
                    span {
                        className = "pw-toolbar-menu-item-label"
                        textContent = item.label
                    }

                    addEventListener("mouseup", { e ->
                        e.stopPropagation()
                        item.onChange(!item.checked.value)
                    })

                    onmouseover = { highlightItemAt(index) }
                }

                is MenuItem.SubMenu -> div {
                    className = "pw-toolbar-menu-item pw-toolbar-menu-item-submenu"
                    dataset.asDynamic().index = index.toString()

                    observeNow(item.visible) { visible ->
                        style.display = if (visible) "flex" else "none"
                    }

                    span {
                        className = "pw-toolbar-menu-item-label"
                        textContent = item.label
                    }
                    icon(Icon.ArrowRight)

                    val subMenuPopup = div {
                        className = "pw-toolbar-menu-submenu-popup"
                        renderMenuItems(item.items)
                    }

                    onmouseover = {
                        highlightItemAt(index)
                        showSubMenu(subMenuPopup)
                    }
                }

                MenuItem.Separator -> div {
                    className = "pw-toolbar-menu-separator"
                }
            }
        }
    }

    private fun onButtonMouseDown(e: MouseEvent) {
        e.stopPropagation()
        justOpened = !menuVisible.value
        menuVisible.value = true
    }

    private fun onButtonMouseUp() {
        if (justOpened) {
            menuElement.focus()
        } else {
            menuVisible.value = false
        }
        justOpened = false
    }

    private fun onButtonKeyDown(e: KeyboardEvent) {
        when (e.key) {
            "Enter", " ", "ArrowDown" -> {
                e.preventDefault()
                e.stopPropagation()
                justOpened = !menuVisible.value
                menuVisible.value = true
                menuElement.focus()
            }
        }
    }

    private fun onMenuMouseUp(e: MouseEvent) {
        val target = e.target
        if (target !is HTMLElement) return

        val itemElement = findMenuItemElement(target) ?: return
        val index = (itemElement.dataset.asDynamic().index as? String)?.toIntOrNull() ?: return

        selectItem(index)
    }

    private fun onMenuKeyDown(e: KeyboardEvent) {
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
                highlightedIndex?.let { selectItem(it) }
            }
            "Escape" -> {
                menuVisible.value = false
            }
        }
    }

    private fun onMenuBlur() {
        // Small delay to allow click events to process
        kotlinx.browser.window.setTimeout({
            if (!menuElement.contains(document.activeElement)) {
                menuVisible.value = false
            }
        }, 100)
    }

    private fun onDocumentMouseDown(e: Event) {
        val target = e.target
        if (target !is Node || !element.contains(target)) {
            menuVisible.value = false
        }
    }

    private fun findMenuItemElement(target: HTMLElement): HTMLElement? {
        var current: HTMLElement? = target
        while (current != null && current != menuElement) {
            if (current.classList.contains("pw-toolbar-menu-item")) {
                return current
            }
            current = current.parentElement as? HTMLElement
        }
        return null
    }

    private fun clearHighlight() {
        highlightedElement?.classList?.remove("pw-toolbar-menu-highlighted")
        highlightedIndex = null
        highlightedElement = null
    }

    private fun highlightItemAt(index: Int) {
        clearHighlight()
        hideAllSubMenus()

        val children = menuElement.children
        for (i in 0 until children.length) {
            val child = children.item(i) as? HTMLElement ?: continue
            if ((child.dataset.asDynamic().index as? String)?.toIntOrNull() == index) {
                highlightedIndex = index
                highlightedElement = child
                child.classList.add("pw-toolbar-menu-highlighted")

                // Show submenu if this is a submenu item
                val subMenuPopup = child.querySelector(".pw-toolbar-menu-submenu-popup") as? HTMLElement
                if (subMenuPopup != null) {
                    showSubMenu(subMenuPopup)
                }
                break
            }
        }
    }

    private fun highlightNext() {
        val selectableIndices = getSelectableIndices()
        if (selectableIndices.isEmpty()) return

        val currentIdx = highlightedIndex
        val nextIdx = when {
            currentIdx == null -> selectableIndices.first()
            else -> selectableIndices.firstOrNull { it > currentIdx } ?: selectableIndices.first()
        }
        highlightItemAt(nextIdx)
    }

    private fun highlightPrev() {
        val selectableIndices = getSelectableIndices()
        if (selectableIndices.isEmpty()) return

        val currentIdx = highlightedIndex
        val prevIdx = when {
            currentIdx == null -> selectableIndices.last()
            else -> selectableIndices.lastOrNull { it < currentIdx } ?: selectableIndices.last()
        }
        highlightItemAt(prevIdx)
    }

    private fun getSelectableIndices(): List<Int> =
        items.mapIndexedNotNull { index, item ->
            when (item) {
                is MenuItem.Separator -> null
                is MenuItem.Action -> if (item.enabled.value) index else null
                is MenuItem.SubMenu -> if (item.visible.value) index else null
                is MenuItem.Check -> index
            }
        }

    private fun selectItem(index: Int) {
        val item = items.getOrNull(index) ?: return

        when (item) {
            is MenuItem.Action -> {
                if (item.enabled.value) {
                    menuVisible.value = false
                    item.onAction()
                }
            }
            is MenuItem.Check -> {
                item.onChange(!item.checked.value)
                // Keep menu open for checkbox items
            }
            is MenuItem.SubMenu -> {
                // Submenus are handled by hover
            }
            MenuItem.Separator -> {
                // Separators are not selectable
            }
        }
    }

    private fun showSubMenu(subMenuPopup: HTMLElement) {
        hideAllSubMenus()
        subMenuPopup.style.display = "block"
        activeSubMenu = subMenuPopup
    }

    private fun hideAllSubMenus() {
        activeSubMenu?.style?.display = "none"
        activeSubMenu = null
    }

    companion object {
        init {
            @Suppress("CssUnusedSymbol", "CssUnresolvedCustomProperty")
            // language=css
            style("""
                .pw-toolbar-menu {
                    position: relative;
                    display: inline-block;
                }

                .pw-toolbar-menu > .pw-button {
                    min-width: 55px;
                    border: none;
                    background: none;
                }

                .pw-toolbar-menu > .pw-button .pw-button-inner {
                    background: none;
                    border: none;
                    padding: 2px 6px;
                }

                .pw-toolbar-menu > .pw-button:hover .pw-button-inner {
                    background-color: hsl(0, 0%, 25%);
                }

                .pw-toolbar-menu > .pw-button:active .pw-button-inner {
                    background-color: hsl(0, 0%, 20%);
                }

                .pw-toolbar-menu-popup {
                    display: none;
                    position: absolute;
                    top: 100%;
                    left: 0;
                    z-index: 1001;
                    min-width: 180px;
                    background-color: var(--pw-control-bg-color);
                    border: var(--pw-control-border);
                    outline: none;
                    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
                }

                .pw-toolbar-menu-item {
                    display: flex;
                    align-items: center;
                    padding: 4px 12px;
                    cursor: pointer;
                    white-space: nowrap;
                }

                .pw-toolbar-menu-item:hover,
                .pw-toolbar-menu-highlighted {
                    background-color: var(--pw-control-bg-color-hover);
                    color: var(--pw-control-text-color-hover);
                }

                .pw-toolbar-menu-item-disabled {
                    opacity: 0.5;
                    cursor: default;
                }

                .pw-toolbar-menu-item-disabled:hover {
                    background-color: transparent;
                    color: inherit;
                }

                .pw-toolbar-menu-item-label {
                    flex: 1;
                }

                .pw-toolbar-menu-item-shortcut {
                    margin-left: 24px;
                    opacity: 0.7;
                    font-size: 0.9em;
                }

                .pw-toolbar-menu-item-check {
                    padding-left: 8px;
                }

                .pw-toolbar-menu-item-check-icon {
                    width: 16px;
                    text-align: center;
                    margin-right: 4px;
                }

                .pw-toolbar-menu-item-submenu {
                    position: relative;
                }

                .pw-toolbar-menu-item-submenu > svg {
                    margin-left: 8px;
                    font-size: 0.8em;
                }

                .pw-toolbar-menu-submenu-popup {
                    display: none;
                    position: absolute;
                    top: 0;
                    left: 100%;
                    z-index: 1002;
                    min-width: 150px;
                    background-color: var(--pw-control-bg-color);
                    border: var(--pw-control-border);
                    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
                }

                .pw-toolbar-menu-separator {
                    height: 1px;
                    margin: 4px 8px;
                    background-color: var(--pw-border-color);
                }
            """.trimIndent())
        }
    }
}