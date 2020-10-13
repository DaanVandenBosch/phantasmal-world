package world.phantasmal.webui.dom

import kotlinx.browser.document
import kotlinx.dom.appendText
import kotlinx.dom.clear
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.disposable
import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.list.ListValChangeEvent

fun <E : Event> disposableListener(
    target: EventTarget,
    type: String,
    listener: (E) -> Unit,
    options: AddEventListenerOptions? = null,
): Disposable {
    @Suppress("UNCHECKED_CAST")
    target.addEventListener(type, listener as (Event) -> Unit, options)

    return disposable {
        target.removeEventListener(type, listener)
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

fun <T> Node.bindChildrenTo(
    list: ListVal<T>,
    createChild: (T, Int) -> Node,
): Disposable {
    fun spliceChildren(index: Int, removedCount: Int, inserted: List<T>) {
        for (i in 1..removedCount) {
            removeChild(childNodes[index].unsafeCast<Node>())
        }

        val frag = document.createDocumentFragment()

        inserted.forEachIndexed { i, value ->
            val child = createChild(value, index + i)

            frag.append(child)
        }

        if (index >= childNodes.length) {
            appendChild(frag)
        } else {
            insertBefore(frag, childNodes[index])
        }
    }

    val observer = list.observeList { change: ListValChangeEvent<T> ->
        when (change) {
            is ListValChangeEvent.Change -> {
                spliceChildren(change.index, change.removed.size, change.inserted)
            }
            is ListValChangeEvent.ElementChange -> {
                // TODO: Update children.
            }
        }
    }

    spliceChildren(0, 0, list.value)

    return disposable {
        observer.dispose()
        clear()
    }
}
