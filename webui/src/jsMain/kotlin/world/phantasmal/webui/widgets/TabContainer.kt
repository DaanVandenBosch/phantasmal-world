package world.phantasmal.webui.widgets

import org.w3c.dom.Node
import world.phantasmal.cell.Cell
import world.phantasmal.cell.eq
import world.phantasmal.cell.trueCell
import world.phantasmal.webui.controllers.Tab
import world.phantasmal.webui.controllers.TabContainerController
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.dom.span

class TabContainer<T : Tab>(
    visible: Cell<Boolean> = trueCell(),
    enabled: Cell<Boolean> = trueCell(),
    private val ctrl: TabContainerController<T>,
    private val createWidget: (T) -> Widget,
) : Widget(visible, enabled) {
    override fun Node.createElement() =
        div {
            className = "pw-tab-container"

            div {
                className = "pw-tab-container-bar"

                for (tab in ctrl.tabs) {
                    span {
                        className = "pw-tab-container-tab"
                        title = tab.title
                        textContent = tab.title

                        observeNow(ctrl.activeTab) {
                            if (it == tab) {
                                classList.add(ACTIVE_CLASS)
                            } else {
                                classList.remove(ACTIVE_CLASS)
                            }
                        }

                        onmousedown = { ctrl.setActiveTab(tab) }
                    }
                }
            }
            div {
                className = "pw-tab-container-panes"

                for (tab in ctrl.tabs) {
                    addChild(
                        LazyLoader(
                            visible = ctrl.activeTab eq tab,
                            createWidget = { createWidget(tab) }
                        )
                    )
                }
            }
        }

    override fun selfAndAncestorsVisibleChanged(visible: Boolean) {
        ctrl.visibleChanged(visible)
    }

    companion object {
        private const val ACTIVE_CLASS = "pw-active"

        init {
            @Suppress("CssUnresolvedCustomProperty", "CssUnusedSymbol")
            // language=css
            style("""
                .pw-tab-container {
                    display: flex;
                    flex-direction: column;
                }

                .pw-tab-container-bar {
                    box-sizing: border-box;
                    height: 26px;
                    min-height: 26px; /* To avoid bar from getting squished when pane content gets larger than pane in Firefox. */
                    padding: 3px 3px 0 3px;
                    border-bottom: var(--pw-border);
                }

                .pw-tab-container-tab {
                    box-sizing: border-box;
                    display: inline-flex;
                    align-items: center;
                    height: calc(100% + 1px);
                    padding: 0 8px;
                    border: var(--pw-border);
                    margin: 0 1px -1px 1px;
                    background-color: var(--pw-tab-bg-color);
                    color: var(--pw-tab-text-color);
                    font-size: 12px;
                }

                .pw-tab-container-tab:hover {
                    background-color: var(--pw-tab-bg-color-hover);
                    color: var(--pw-tab-text-color-hover);
                }

                .pw-tab-container-tab.pw-active {
                    background-color: var(--pw-tab-bg-color-active);
                    color: var(--pw-tab-text-color-active);
                    border-bottom-color: var(--pw-tab-bg-color-active);
                }

                .pw-tab-container-panes {
                    flex-grow: 1;
                    display: grid;
                    grid-template: 100% / 100%;
                    overflow: hidden;
                }
            """.trimIndent())
        }
    }
}
