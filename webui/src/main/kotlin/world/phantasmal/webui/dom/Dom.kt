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
import world.phantasmal.core.unsafe.UnsafeMap
import world.phantasmal.core.unsafe.getOrPut
import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.list.ListCell
import world.phantasmal.observable.cell.list.ListChange
import world.phantasmal.observable.cell.list.ListChangeEvent
import world.phantasmal.webui.externals.fontawesome.IconDefinition
import world.phantasmal.webui.externals.fontawesome.freeBrandsSvgIcons.faGithub
import world.phantasmal.webui.externals.fontawesome.freeRegularSvgIcons.faCaretSquareRight
import world.phantasmal.webui.externals.fontawesome.freeRegularSvgIcons.faEye
import world.phantasmal.webui.externals.fontawesome.freeSolidSvgIcons.*
import world.phantasmal.webui.externals.fontawesome.icon as faIcon

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
    classList.add("pw-root")
    return this
}

fun getRoot(): HTMLElement = document.getElementById("pw-root") as HTMLElement

enum class Icon {
    ArrowDown,
    ArrowRight,
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

/** Fontawesome icons. */
private val faElementCache = UnsafeMap<IconDefinition, Element>()

fun Node.icon(icon: Icon): HTMLElement {
    val iconDef = when (icon) {
        Icon.ArrowDown -> faArrowDown
        Icon.ArrowRight -> faArrowRight
        Icon.Eye -> faEye
        Icon.File -> faFile
        Icon.GitHub -> faGithub
        Icon.LevelDown -> faLevelDownAlt
        Icon.LevelUp -> faLevelUpAlt
        Icon.LongArrowRight -> faLongArrowAltRight
        Icon.NewFile -> faFileMedical
        Icon.Play -> faPlay
        Icon.Plus -> faPlus
        Icon.Redo -> faRedo
        Icon.Remove -> faTrashAlt
        Icon.Save -> faSave
        Icon.Stop -> faStop
        Icon.SquareArrowRight -> faCaretSquareRight
        Icon.TriangleDown -> faCaretDown
        Icon.TriangleUp -> faCaretUp
        Icon.Undo -> faUndo
    }

    return span {
        val iconEl = faElementCache.getOrPut(iconDef) { faIcon(iconDef).node[0]!! }
        append(iconEl.cloneNode(deep = true))
    }
}

fun <T> bindChildrenTo(
    parent: Element,
    list: Cell<List<T>>,
    createChild: Node.(T, index: Int) -> Node,
): Disposable =
    if (list is ListCell) {
        bindChildrenTo(parent, list, createChild)
    } else {
        bindChildrenTo(parent, list, createChild, childrenRemoved = { /* Do nothing. */ })
    }

fun <T> bindDisposableChildrenTo(
    parent: Element,
    list: Cell<List<T>>,
    createChild: Node.(T, index: Int) -> Pair<Node, Disposable>,
): Disposable =
    if (list is ListCell) {
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
            },
        )

        disposable {
            disposer.dispose()
            listObserver.dispose()
        }
    }

fun <T> bindChildrenTo(
    parent: Element,
    list: ListCell<T>,
    createChild: Node.(T, index: Int) -> Node,
    after: (ListChangeEvent<T>) -> Unit = {},
): Disposable =
    bindChildrenTo(
        parent,
        list,
        createChild,
        childrenRemoved = { _, _ ->
            // Do nothing.
        },
        after,
    )

fun <T> bindDisposableChildrenTo(
    parent: Element,
    list: ListCell<T>,
    createChild: Node.(T, index: Int) -> Pair<Node, Disposable>,
    after: (ListChangeEvent<T>) -> Unit = {},
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
        },
        after,
    )

    return disposable {
        disposer.dispose()
        listObserver.dispose()
    }
}

private fun <T> bindChildrenTo(
    parent: Element,
    list: Cell<List<T>>,
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
    list: ListCell<T>,
    createChild: Node.(T, index: Int) -> Node,
    childrenRemoved: (index: Int, count: Int) -> Unit,
    after: (ListChangeEvent<T>) -> Unit,
): Disposable =
    list.observeList(callNow = true) { event: ListChangeEvent<T> ->
        for (change in event.changes) {
            if (change is ListChange.Structural) {
                if (change.allRemoved) {
                    parent.innerHTML = ""
                } else {
                    repeat(change.removed.size) {
                        parent.removeChild(parent.childNodes[change.index].unsafeCast<Node>())
                    }
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

        after(event)
    }
