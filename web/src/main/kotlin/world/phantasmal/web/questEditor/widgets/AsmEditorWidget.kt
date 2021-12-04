package world.phantasmal.web.questEditor.widgets

import org.w3c.dom.Node
import world.phantasmal.core.disposable.disposable
import world.phantasmal.web.externals.monacoEditor.*
import world.phantasmal.web.questEditor.asm.monaco.EditorHistory
import world.phantasmal.web.questEditor.controllers.AsmEditorController
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.obj
import world.phantasmal.webui.widgets.Widget

class AsmEditorWidget(private val ctrl: AsmEditorController) : Widget() {
    private lateinit var editor: IStandaloneCodeEditor

    override fun Node.createElement() =
        div {
            className = "pw-quest-editor-asm-editor"

            editor = create(this, obj {
                theme = "phantasmal-world"
                scrollBeyondLastLine = false
                autoIndent = "full"
                fontSize = 13
                wordWrap = "on"
                wrappingIndent = "indent"
                renderIndentGuides = false
                folding = false
                wordBasedSuggestions = false
                occurrencesHighlight = true
                fixedOverflowWidgets = true
            })

            addDisposable(disposable { editor.dispose() })

            observeNow(ctrl.textModel) { editor.setModel(it) }

            observeNow(ctrl.readOnly) { editor.updateOptions(obj { readOnly = it }) }

            addDisposable(size.observeChange { (size) ->
                if (size.width > .0 && size.height > .0) {
                    editor.layout(obj {
                        width = size.width
                        height = size.height
                    })
                }
            })

            // Add VSCode keybinding for command palette.
            val quickCommand = editor.getAction("editor.action.quickCommand")

            editor.addAction(object : IActionDescriptor {
                override var id = "editor.action.quickCommand"
                override var label = "Command Palette"
                override var keybindings =
                    arrayOf(KeyMod.CtrlCmd or KeyMod.Shift or KeyCode.KEY_P)

                override fun run(editor: ICodeEditor, vararg args: dynamic) {
                    quickCommand.run()
                }
            })

            // Undo/redo.
            observe(ctrl.didUndo) {
                editor.focus()
                editor.trigger(
                    source = AsmEditorWidget::class.simpleName,
                    handlerId = "undo",
                    payload = undefined,
                )
            }

            observe(ctrl.didRedo) {
                editor.focus()
                editor.trigger(
                    source = AsmEditorWidget::class.simpleName,
                    handlerId = "redo",
                    payload = undefined,
                )
            }

            editor.onDidFocusEditorWidget(ctrl::makeUndoCurrent)

            addDisposable(EditorHistory(editor))
        }

    override fun focus() {
        editor.focus()
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

            @Suppress("CssUnusedSymbol")
            // language=css
            style(
                """
                .pw-quest-editor-asm-editor {
                    flex-grow: 1;
                }
                .pw-quest-editor-asm-editor .editor-widget {
                    z-index: 30;
                }
            """.trimIndent())
        }
    }
}
