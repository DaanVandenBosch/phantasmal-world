package world.phantasmal.webui.dom

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.appendText
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import org.w3c.dom.pointerevents.PointerEvent
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.disposable

fun <E : Event> EventTarget.disposableListener(
    type: String,
    listener: (E) -> Unit,
    options: AddEventListenerOptions? = null,
): Disposable {
    @Suppress("UNCHECKED_CAST")
    addEventListener(type, listener as (Event) -> Unit, options)

    return disposable {
        removeEventListener(type, listener)
    }
}

fun <E : Event> EventTarget.disposableListener(
    type: String,
    listener: (E) -> Unit,
    useCapture: Boolean,
): Disposable {
    @Suppress("UNCHECKED_CAST")
    addEventListener(type, listener as (Event) -> Unit, useCapture)

    return disposable {
        removeEventListener(type, listener)
    }
}

fun Element.disposablePointerDrag(
    onPointerDown: (e: PointerEvent) -> Boolean,
    onPointerMove: (movedX: Int, movedY: Int, e: PointerEvent) -> Boolean,
    onPointerUp: (e: PointerEvent) -> Unit = {},
): Disposable {
    var prevPointerX: Int
    var prevPointerY: Int
    var windowMoveListener: Disposable? = null
    var windowUpListener: Disposable? = null

    val downListener = disposableListener<PointerEvent>("pointerdown", { downEvent ->
        if (onPointerDown(downEvent)) {
            prevPointerX = downEvent.clientX
            prevPointerY = downEvent.clientY

            windowMoveListener =
                window.disposableListener<PointerEvent>("pointermove", { moveEvent ->
                    val movedX = moveEvent.clientX - prevPointerX
                    val movedY = moveEvent.clientY - prevPointerY
                    prevPointerX = moveEvent.clientX
                    prevPointerY = moveEvent.clientY

                    if (!onPointerMove(movedX, movedY, moveEvent)) {
                        windowMoveListener?.dispose()
                        windowUpListener?.dispose()
                    }
                })

            windowUpListener =
                window.disposableListener<PointerEvent>("pointerup", { upEvent ->
                    onPointerUp(upEvent)
                    windowMoveListener?.dispose()
                    windowUpListener?.dispose()
                })
        }
    })

    return disposable {
        downListener.dispose()
        windowMoveListener?.dispose()
        windowUpListener?.dispose()
    }
}

fun HTMLElement.root(): HTMLElement {
    val styleEl = document.createElement("style") as HTMLStyleElement
    styleEl.id = "pw-root-styles"
    styleEl.appendText(DEFAULT_STYLE)
    document.head!!.append(styleEl)

    id = "pw-root"
    return this
}

fun getRoot(): HTMLElement = document.getElementById("pw-root") as HTMLElement

enum class Icon {
    ArrowDown,
    Eye,
    File,
    GitHub,
    LevelDown,
    LevelUp,
    LongArrowRight,
    NewFile,
    Play,
    Plus,
    Redo,
    Remove,
    Save,
    SquareArrowRight,
    Stop,
    TriangleDown,
    TriangleUp,
    Undo,
}

fun Node.icon(icon: Icon): HTMLElement {
    val iconStr = when (icon) {
        Icon.ArrowDown -> "fas fa-arrow-down"
        Icon.Eye -> "far fa-eye"
        Icon.File -> "fas fa-file"
        Icon.GitHub -> "fab fa-github"
        Icon.LevelDown -> "fas fa-level-down-alt"
        Icon.LevelUp -> "fas fa-level-up-alt"
        Icon.LongArrowRight -> "fas fa-long-arrow-alt-right"
        Icon.NewFile -> "fas fa-file-medical"
        Icon.Play -> "fas fa-play"
        Icon.Plus -> "fas fa-plus"
        Icon.Redo -> "fas fa-redo"
        Icon.Remove -> "fas fa-trash-alt"
        Icon.Save -> "fas fa-save"
        Icon.Stop -> "fas fa-stop"
        Icon.SquareArrowRight -> "far fa-caret-square-right"
        Icon.TriangleDown -> "fas fa-caret-down"
        Icon.TriangleUp -> "fas fa-caret-up"
        Icon.Undo -> "fas fa-undo"
    }

    // Wrap the span in another span, because Font Awesome will replace the inner element. This way
    // the returned element will stay valid.
    return span { span { className = iconStr } }
}
