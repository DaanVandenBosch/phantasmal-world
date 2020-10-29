package world.phantasmal.webui.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.Node
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal
import world.phantasmal.webui.controllers.Tab
import world.phantasmal.webui.controllers.TabController
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.dom.span

class TabContainer<T : Tab>(
    scope: CoroutineScope,
    hidden: Val<Boolean> = falseVal(),
    disabled: Val<Boolean> = falseVal(),
    private val ctrl: TabController<T>,
    private val createWidget: (CoroutineScope, T) -> Widget,
) : Widget(scope, hidden, disabled) {
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

                        observe(ctrl.activeTab) {
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
                            scope,
                            hidden = ctrl.activeTab.transform { it != tab },
                            createWidget = { scope -> createWidget(scope, tab) }
                        )
                    )
                }
            }
        }

    init {
        observe(selfOrAncestorHidden, ctrl::hiddenChanged)
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
                    height: 28px;
                    min-height: 28px; /* To avoid bar from getting squished when pane content gets larger than pane in Firefox. */
                    padding: 3px 3px 0 3px;
                    border-bottom: var(--pw-border);
                }

                .pw-tab-container-tab {
                    box-sizing: border-box;
                    display: inline-flex;
                    align-items: center;
                    height: calc(100% + 1px);
                    padding: 0 10px;
                    border: var(--pw-border);
                    margin: 0 1px -1px 1px;
                    background-color: var(--pw-tab-bg-color);
                    color: var(--pw-tab-text-color);
                    font-size: 13px;
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
                    display: flex;
                    flex-direction: row;
                    overflow: hidden;
                }

                .pw-tab-container-panes > * {
                    flex-grow: 1;
                }
            """.trimIndent())
        }
    }
}
