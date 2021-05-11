package world.phantasmal.webui.widgets

import org.w3c.dom.Node
import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.trueCell
import world.phantasmal.webui.dom.div

/**
 * Takes ownership of the given [children].
 */
class Toolbar(
    visible: Cell<Boolean> = trueCell(),
    enabled: Cell<Boolean> = trueCell(),
    children: List<Widget>,
) : Widget(visible, enabled) {
    private val childWidgets = children

    override fun Node.createElement() =
        div {
            className = "pw-toolbar"

            childWidgets.forEach { child ->
                // Group labelled controls and their labels together.
                if (child is LabelledControl && child.label != null) {
                    div {
                        className = "pw-toolbar-group"

                        when (child.preferredLabelPosition) {
                            LabelPosition.Before -> {
                                addChild(child.label!!)
                                addChild(child)
                            }
                            LabelPosition.After -> {
                                addChild(child)
                                addChild(child.label!!)
                            }
                        }
                    }
                } else {
                    addChild(child)
                }
            }
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol", "CssUnresolvedCustomProperty")
            // language=css
            style("""
                .pw-toolbar {
                    box-sizing: border-box;
                    display: flex;
                    flex-direction: row;
                    align-items: center;
                    border-bottom: var(--pw-border);
                    padding: 3px 2px;
                }

                .pw-toolbar > * {
                    margin: 0 1px;
                }

                .pw-toolbar > .pw-toolbar-group {
                    margin: 0 3px;
                    display: flex;
                    flex-direction: row;
                    align-items: center;
                }

                .pw-toolbar > .pw-toolbar-group > * {
                    margin: 0 2px;
                }

                .pw-toolbar .pw-input {
                    height: 24px;
                }
            """.trimIndent())
        }
    }
}
