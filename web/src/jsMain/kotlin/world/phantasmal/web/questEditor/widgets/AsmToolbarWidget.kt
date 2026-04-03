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
                    // Previously included an "Inline args" checkbox that toggled whether
                    // push arguments were inlined into Pop opcodes. Stack args are now
                    // normalized unconditionally at parse time (see normalizeStackArgs in
                    // Bytecode.kt), so the toggle is no longer needed.
                    children = listOf(
                        Checkbox(
                            enabled = ctrl.hexFormatEnabled,
                            label = "Hex",
                            checked = ctrl.hexFormat,
                            onChange = ctrl::setHexFormat,
                        ),
                        Checkbox(
                            enabled = ctrl.hideNopsEnabled,
                            label = "Hide NOPs",
                            checked = ctrl.hideNops,
                            onChange = ctrl::setHideNops,
                        ),
                    )
                )
            )
        }
}
