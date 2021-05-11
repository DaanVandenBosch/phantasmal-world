package world.phantasmal.web.questEditor.widgets

import org.w3c.dom.Node
import world.phantasmal.web.questEditor.controllers.AsmEditorController
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.Widget

class AsmWidget(private val ctrl: AsmEditorController) : Widget() {
    private lateinit var editorWidget: AsmEditorWidget

    override fun Node.createElement() =
        div {
            className = "pw-quest-editor-asm"

            addChild(AsmToolbarWidget(ctrl))
            editorWidget = addChild(AsmEditorWidget(ctrl))
        }

    override fun focus() {
        editorWidget.focus()
    }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-quest-editor-asm {
                    display: flex;
                    flex-direction: column;
                    overflow: hidden;
                }
            """.trimIndent())
        }
    }
}
