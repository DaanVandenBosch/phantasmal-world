package world.phantasmal.webui.dom

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.appendText
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import org.w3c.dom.pointerevents.PointerEvent
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.Disposer
import world.phantasmal.core.disposable.disposable
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.list.ListValChangeEvent

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

fun <T> bindChildrenTo(
    parent: Element,
    list: Val<List<T>>,
    createChild: Node.(T, index: Int) -> Node,
): Disposable =
    if (list is ListVal) {
        bindChildrenTo(parent, list, createChild)
    } else {
        bindChildrenTo(parent, list, createChild, childrenRemoved = { /* Do nothing. */ })
    }

fun <T> bindDisposableChildrenTo(
    parent: Element,
    list: Val<List<T>>,
    createChild: Node.(T, index: Int) -> Pair<Node, Disposable>,
): Disposable =
    if (list is ListVal) {
        bindDisposableChildrenTo(parent, list, createChild)
    } else {
        val disposer = Disposer()

        val listObserver = bindChildrenTo(
            parent,
            list,
            createChild = { item, index ->
                val (child, disposable) = createChild(item, index)
                disposer.add(disposable)
                child
            },
            childrenRemoved = {
                disposer.disposeAll()
            }
        )

        disposable {
            disposer.dispose()
            listObserver.dispose()
        }
    }

fun <T> bindChildrenTo(
    parent: Element,
    list: ListVal<T>,
    createChild: Node.(T, index: Int) -> Node,
): Disposable =
    bindChildrenTo(
        parent,
        list,
        createChild,
        childrenRemoved = { _, _ ->
            // Do nothing.
        }
    )

fun <T> bindDisposableChildrenTo(
    parent: Element,
    list: ListVal<T>,
    createChild: Node.(T, index: Int) -> Pair<Node, Disposable>,
): Disposable {
    val disposer = Disposer()

    val listObserver = bindChildrenTo(
        parent,
        list,
        createChild = { value, index ->
            val (child, disposable) = createChild(value, index)
            disposer.add(index, disposable)
            child
        },
        childrenRemoved = { index, count ->
            disposer.removeAt(index, count)
        }
    )

    return disposable {
        disposer.dispose()
        listObserver.dispose()
    }
}

private fun <T> bindChildrenTo(
    parent: Element,
    list: Val<List<T>>,
    createChild: Node.(T, index: Int) -> Node,
    childrenRemoved: () -> Unit,
): Disposable =
    list.observe(callNow = true) { (items) ->
        parent.innerHTML = ""
        childrenRemoved()

        val frag = document.createDocumentFragment()

        items.forEachIndexed { i, item ->
            frag.appendChild(frag.createChild(item, i))
        }

        parent.appendChild(frag)
    }

private fun <T> bindChildrenTo(
    parent: Element,
    list: ListVal<T>,
    createChild: Node.(T, index: Int) -> Node,
    childrenRemoved: (index: Int, count: Int) -> Unit,
): Disposable =
    list.observeList(callNow = true) { change: ListValChangeEvent<T> ->
        if (change is ListValChangeEvent.Change) {
            repeat(change.removed.size) {
                parent.removeChild(parent.childNodes[change.index].unsafeCast<Node>())
            }

            childrenRemoved(change.index, change.removed.size)

            val frag = document.createDocumentFragment()

            change.inserted.forEachIndexed { i, value ->
                frag.appendChild(frag.createChild(value, change.index + i))
            }

            if (change.index >= parent.childNodes.length) {
                parent.appendChild(frag)
            } else {
                parent.insertBefore(frag, parent.childNodes[change.index])
            }
        }
    }
