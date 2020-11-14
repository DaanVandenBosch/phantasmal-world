package world.phantasmal.web.questEditor.widgets

import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.Node
import world.phantasmal.core.disposable.disposable
import world.phantasmal.web.externals.monacoEditor.IStandaloneCodeEditor
import world.phantasmal.web.externals.monacoEditor.create
import world.phantasmal.web.externals.monacoEditor.defineTheme
import world.phantasmal.web.externals.monacoEditor.set
import world.phantasmal.web.questEditor.controllers.AssemblyEditorController
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.obj
import world.phantasmal.webui.widgets.Widget

class AssemblyEditorWidget(
    scope: CoroutineScope,
    private val ctrl: AssemblyEditorController,
) : Widget(scope) {
    private lateinit var editor: IStandaloneCodeEditor

    override fun Node.createElement() =
        div {
            editor = create(this, obj {
                theme = "phantasmal-world"
                scrollBeyondLastLine = false
                autoIndent = "full"
                fontSize = 13
                wordWrap = "on"
                wrappingIndent = "indent"
                renderIndentGuides = false
                folding = false
            })

            addDisposable(disposable { editor.dispose() })

            observe(ctrl.textModel) { editor.setModel(it) }

            observe(ctrl.readOnly) { editor.updateOptions(obj { readOnly = it }) }

            addDisposable(size.observe { (size) ->
                editor.layout(obj {
                    width = size.width
                    height = size.height
                })
            })
        }

    companion object {
        init {
            defineTheme("phantasmal-world", obj {
                base = "vs-dark"
                inherit = true
                rules = arrayOf(
                    obj { token = ""; foreground = "E0E0E0"; background = "#181818" },
                    obj { token = "tag"; foreground = "99BBFF" },
                    obj { token = "keyword"; foreground = "D0A0FF"; fontStyle = "bold" },
                    obj { token = "predefined"; foreground = "BBFFBB" },
                    obj { token = "number"; foreground = "FFFFAA" },
                    obj { token = "number.hex"; foreground = "FFFFAA" },
                    obj { token = "string"; foreground = "88FFFF" },
                    obj { token = "string.escape"; foreground = "8888FF" },
                )
                colors = obj {
                    this["editor.background"] = "#181818"
                    this["editor.lineHighlightBackground"] = "#202020"
                }
            })
        }
    }
}
