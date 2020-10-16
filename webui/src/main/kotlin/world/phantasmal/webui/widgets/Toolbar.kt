package world.phantasmal.webui.widgets

import org.w3c.dom.Node
import world.phantasmal.core.disposable.Scope
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal
import world.phantasmal.webui.dom.div

class Toolbar(
    scope: Scope,
    hidden: Val<Boolean> = falseVal(),
    disabled: Val<Boolean> = falseVal(),
    children: List<Widget>,
) : Widget(scope, ::style, hidden, disabled) {
    private val childWidgets = children

    override fun Node.createElement() =
        div(className = "pw-toolbar") {
            childWidgets.forEach { child ->
                // Group labelled controls and their labels together.
                if (child is LabelledControl && child.label != null) {
                    div(className = "pw-toolbar-group") {
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
}

@Suppress("CssUnusedSymbol", "CssUnresolvedCustomProperty")
// language=css
private fun style() = """
.pw-toolbar {
    box-sizing: border-box;
    display: flex;
    flex-direction: row;
    align-items: center;
    border-bottom: var(--pw-border);
    padding: 0 2px;
}

.pw-toolbar > * {
    margin: 2px 1px;
}

.pw-toolbar > .pw-toolbar-group {
    margin: 2px 3px;
    display: flex;
    flex-direction: row;
    align-items: center;
}

.pw-toolbar > .pw-toolbar-group > * {
    margin: 0 2px;
}

.pw-toolbar .pw-input {
    height: 26px;
}
"""
