package world.phantasmal.web.questEditor.asm.monaco

import world.phantasmal.web.externals.monacoEditor.*
import world.phantasmal.webui.DisposableContainer
import kotlin.math.abs

class EditorHistory(private val editor: IStandaloneCodeEditor) : DisposableContainer() {
    private val history = mutableListOf<IPosition>()
    private var historyIndex = -1
    private var captureHistory = true

    init {
        addDisposables(
            editor.onDidChangeCursorPosition(::onDidChangeCursorPosition).toDisposable(),

            editor.addAction(object : IActionDescriptor {
                override var id = "phantasmal.action.back"
                override var label = "Go Back"
                override var keybindings = arrayOf(KeyMod.Alt or KeyCode.LeftArrow)

                override fun run(editor: ICodeEditor, vararg args: dynamic) {
                    goBack()
                }
            }).toDisposable(),

            editor.addAction(object : IActionDescriptor {
                override var id = "phantasmal.action.forward"
                override var label = "Go Forward"
                override var keybindings = arrayOf(KeyMod.Alt or KeyCode.RightArrow)

                override fun run(editor: ICodeEditor, vararg args: dynamic) {
                    goForward()
                }
            }).toDisposable(),

            editor.onMouseUp(::onMouseUp).toDisposable(),

            editor.onDidChangeModel(::reset).toDisposable(),
        )
    }

    private fun reset() {
        history.clear()
        historyIndex = -1
    }

    private fun onDidChangeCursorPosition(e: ICursorPositionChangedEvent) {
        if (!captureHistory) return

        while (history.lastIndex > historyIndex) {
            history.removeLast()
        }

        if (
            e.source === "api" ||
            historyIndex == -1 ||
            abs(e.position.lineNumber - history[historyIndex].lineNumber) >= 10
        ) {
            history.add(e.position.unsafeCast<IPosition>())
            historyIndex++
        } else {
            history[historyIndex] = e.position.unsafeCast<IPosition>()
        }
    }

    private fun goBack() {
        if (historyIndex > 0) {
            setPosition(history[--historyIndex])
        }
    }

    private fun goForward() {
        if (historyIndex < history.lastIndex) {
            setPosition(history[++historyIndex])
        }
    }

    private fun setPosition(position: IPosition) {
        captureHistory = false
        editor.setPosition(position)
        editor.revealPositionInCenterIfOutsideViewport(position, ScrollType.Immediate)
        captureHistory = true
    }

    private fun onMouseUp(e: IEditorMouseEvent) {
        val button = e.event.browserEvent.button.toInt()
        val buttons = e.event.browserEvent.buttons.toInt()

        if (button == 3) {
            if (buttons == 0) {
                e.event.preventDefault()
                goBack()
            }

            editor.focus()
        } else if (button == 4) {
            if (buttons == 0) {
                e.event.preventDefault()
                goForward()
            }

            editor.focus()
        }
    }
}
