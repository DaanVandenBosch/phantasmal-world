package world.phantasmal.web.core.widgets

import org.w3c.dom.Node
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal
import world.phantasmal.web.core.controllers.Tab
import world.phantasmal.web.core.controllers.TabController
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.dom.span
import world.phantasmal.webui.widgets.LazyLoader
import world.phantasmal.webui.widgets.Widget

class TabContainer<T : Tab>(
    hidden: Val<Boolean> = falseVal(),
    private val ctrl: TabController<T>,
    private val createWidget: (T) -> Widget,
) : Widget(::style, hidden) {
    override fun Node.createElement() = div(className = "pw-tab-container") {
        div(className = "pw-tab-container-bar") {
            for (tab in ctrl.tabs) {
                span(
                    className = "pw-tab-container-tab",
                    title = tab.title,
                ) {
                    textContent = tab.title

                    observe(ctrl.activeTab) {
                        if (it == tab) {
                            classList.add("active")
                        } else {
                            classList.remove("active")
                        }
                    }

                    onmousedown = { ctrl.setActiveTab(tab) }
                }
            }
        }
        div(className = "pw-tab-container-panes") {
            for (tab in ctrl.tabs) {
                addChild(
                    LazyLoader(
                        hidden = ctrl.activeTab.transform { it != tab },
                        createWidget = { createWidget(tab) }
                    )
                )
            }
        }
    }

    init {
        observe(selfOrAncestorHidden, ctrl::hiddenChanged)
    }
}

@Suppress("CssUnresolvedCustomProperty", "CssUnusedSymbol")
// language=css
private fun style() = """
.pw-tab-container-bar {
    box-sizing: border-box;
    height: 28px;
    padding: 3px 3px 0 3px;
    border-bottom: var(--border);
}

.pw-tab-container-tab {
    box-sizing: border-box;
    display: inline-flex;
    align-items: center;
    height: calc(100% + 1px);
    padding: 0 10px;
    border: var(--border);
    margin: 0 1px -1px 1px;
    background-color: hsl(0, 0%, 12%);
    color: hsl(0, 0%, 75%);
    font-size: 13px;
}

.pw-tab-container-tab:hover {
    background-color: hsl(0, 0%, 18%);
    color: hsl(0, 0%, 85%);
}

.pw-tab-container-tab.active {
    background-color: var(--bg-color);
    color: hsl(0, 0%, 90%);
    border-bottom-color: var(--bg-color);
}
"""
