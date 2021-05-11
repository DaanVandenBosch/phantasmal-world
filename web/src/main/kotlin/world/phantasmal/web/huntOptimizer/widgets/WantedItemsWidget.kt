package world.phantasmal.web.huntOptimizer.widgets

import org.w3c.dom.Node
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.web.huntOptimizer.controllers.WantedItemsController
import world.phantasmal.webui.dom.*
import world.phantasmal.webui.widgets.Button
import world.phantasmal.webui.widgets.ComboBox
import world.phantasmal.webui.widgets.IntInput
import world.phantasmal.webui.widgets.Widget

class WantedItemsWidget(private val ctrl: WantedItemsController) : Widget() {
    override fun Node.createElement() =
        div {
            className = "pw-hunt-optimizer-wanted-items"

            h2 { textContent = "Wanted Items" }
            addChild(ComboBox(
                items = ctrl.selectableItems,
                itemToString = { it.name },
                placeholderText = "Add an item",
                onSelect = ctrl::addWantedItem,
                filter = ctrl::filterSelectableItems,
            ))
            div {
                className = "pw-hunt-optimizer-wanted-items-table-wrapper"

                table {
                    bindDisposableChildrenTo(ctrl.wantedItems) { wanted, _ ->
                        val disposer = Disposer()

                        val node = tr {
                            td {
                                addChild(disposer.add(IntInput(
                                    value = wanted.amount,
                                    onChange = { ctrl.setAmount(wanted, it) },
                                    min = 0,
                                    max = 1_000,
                                    step = 1,
                                )), addToDisposer = false)
                            }
                            td { textContent = wanted.itemType.name }
                            td {
                                addChild(disposer.add(Button(
                                    iconLeft = Icon.Remove,
                                    onClick = { ctrl.removeWantedItem(wanted) },
                                )), addToDisposer = false)
                            }
                        }

                        Pair(node, disposer)
                    }
                }
            }
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-hunt-optimizer-wanted-items {
                    display: flex;
                    flex-direction: column;
                    align-items: stretch;
                    padding: 0 6px;
                    min-width: 220px;
                }

                .pw-hunt-optimizer-wanted-items-table-wrapper {
                    flex-grow: 1;
                    width: calc(100% + 6px);
                    overflow: auto;
                    margin: 4px -3px;
                }

                .pw-hunt-optimizer-wanted-items-table-wrapper > table {
                    width: 100%;
                    border-collapse: collapse;
                }

                .pw-hunt-optimizer-wanted-items-table-wrapper td {
                    padding: 1px 3px;
                }
            """.trimIndent())
        }
    }
}
