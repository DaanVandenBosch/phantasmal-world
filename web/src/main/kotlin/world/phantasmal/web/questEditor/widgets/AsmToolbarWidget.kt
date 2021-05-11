package world.phantasmal.web.questEditor.widgets

import org.w3c.dom.Node
import world.phantasmal.web.questEditor.controllers.AsmEditorController
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.Checkbox
import world.phantasmal.webui.widgets.Toolbar
import world.phantasmal.webui.widgets.Widget

class AsmToolbarWidget(private val ctrl: AsmEditorController) : Widget() {
    override fun Node.createElement() =
        div {
            className = "pw-quest-editor-asm-toolbar"

            addChild(
                Toolbar(
                    enabled = ctrl.enabled,
                    children = listOf(
                        Checkbox(
                            enabled = ctrl.inlineStackArgsEnabled,
                            tooltip = ctrl.inlineStackArgsTooltip,
                        label = "Inline args",
                        checked = ctrl.inlineStackArgs,
                        onChange = ctrl::setInlineStackArgs,
                    )
                )
            ))
        }
}
