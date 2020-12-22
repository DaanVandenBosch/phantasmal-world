package world.phantasmal.webui.widgets

import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.get
import org.w3c.dom.pointerevents.PointerEvent
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.emptyStringVal
import world.phantasmal.observable.value.isEmpty
import world.phantasmal.observable.value.trueVal
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.dom.dom
import world.phantasmal.webui.dom.h1
import world.phantasmal.webui.dom.section

open class Dialog(
    visible: Val<Boolean> = trueVal(),
    enabled: Val<Boolean> = trueVal(),
    private val title: Val<String>,
    private val description: Val<String> = emptyStringVal(),
    private val content: Node.() -> Unit = {},
    private val footer: Node.() -> Unit = {},
    protected val onDismiss: () -> Unit = {},
) : Widget(visible, enabled) {
    private var x = 0
    private var y = 0

    private var overlayElement = dom {
        div {
            className = "pw-dialog-modal-overlay"
            tabIndex = -1

            addEventListener("focus", { this@Dialog.focus() })
        }
    }

    val dialogElement = dom {
        section {
            className = "pw-dialog"
            tabIndex = 0
            style.width = "${WIDTH}px"
            style.maxHeight = "${MAX_HEIGHT}px"

            addEventListener("keydown", ::onKeyDown)

            h1 {
                text(this@Dialog.title)

                onDrag(
                    onPointerDown = { true },
                    onPointerMove = ::onPointerMove,
                    onPointerUp = { it.preventDefault() },
                )
            }
            div {
                className = "pw-dialog-description"
                hidden(description.isEmpty())
                text(description)
            }
            div {
                className = "pw-dialog-body"
                content()
            }
            div {
                className = "pw-dialog-footer"
                footer()
            }
        }
    }

    init {
        observe(visible) {
            if (it) {
                setPosition(
                    (window.innerWidth - WIDTH) / 2,
                    (window.innerHeight - MAX_HEIGHT) / 2,
                )
                window.document.body!!.append(overlayElement)
                window.document.body!!.append(dialogElement)
                focus()
            } else {
                dialogElement.remove()
                overlayElement.remove()
            }
        }
    }

    override fun Node.createElement() = div { className = "pw-dialog-stub" }

    override fun internalDispose() {
        dialogElement.remove()
        overlayElement.remove()
        super.internalDispose()
    }

    private fun onPointerMove(movedX: Int, movedY: Int, e: PointerEvent): Boolean {
        e.preventDefault()
        setPosition(this.x + movedX, this.y + movedY)
        return true
    }

    private fun setPosition(x: Int, y: Int) {
        this.x = x
        this.y = y
        dialogElement.style.transform = "translate(${x}px, ${y}px)"
    }

    override fun focus() {
        (firstFocusableChild(dialogElement) ?: dialogElement).focus()
    }

    private fun firstFocusableChild(parent: HTMLElement): HTMLElement? {
        for (i in 0 until parent.children.length) {
            val child = parent.children[i]

            if (child is HTMLElement) {
                if (child.tabIndex >= 0) {
                    return child
                } else {
                    firstFocusableChild(child)?.let {
                        return it
                    }
                }
            }
        }

        return null
    }

    private fun onKeyDown(e: Event) {
        e as KeyboardEvent

        if (e.key == "Escape") {
            onDismiss()
        }
    }

    companion object {
        private const val WIDTH = 500
        private const val MAX_HEIGHT = 500

        init {
            @Suppress("CssUnresolvedCustomProperty", "CssUnusedSymbol")
            // language=css
            style("""
                .pw-dialog {
                    z-index: 20;
                    display: flex;
                    flex-direction: column;
                    outline: none;
                    position: fixed;
                    left: 0;
                    top: 0;
                    background-color: var(--pw-bg-color);
                    border: var(--pw-border);
                    padding: 10px;
                    box-shadow: black 0 0 10px -2px;
                }

                .pw-dialog:focus-within {
                    border: var(--pw-border-focus);
                }

                .pw-dialog h1 {
                    font-size: 20px;
                    margin: 0 0 10px 0;
                    padding-bottom: 4px;
                    border-bottom: var(--pw-border);
                }

                .pw-dialog-description {
                    user-select: text;
                    cursor: text;
                }

                .pw-dialog-body {
                    flex: 1;
                    margin: 4px 0;
                }

                .pw-dialog-footer {
                    display: flex;
                    flex-direction: row;
                    justify-content: flex-end;
                }

                .pw-dialog-footer > * {
                    margin-left: 2px;
                }

                .pw-dialog-modal-overlay {
                    outline: none;
                    z-index: 10;
                    position: fixed;
                    top: 0;
                    bottom: 0;
                    left: 0;
                    right: 0;
                    background-color: black;
                    opacity: 50%;
                    backdrop-filter: blur(5px);
                }
            """.trimIndent())
        }
    }
}
