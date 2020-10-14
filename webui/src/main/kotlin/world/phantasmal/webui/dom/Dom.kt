package world.phantasmal.webui.dom

import kotlinx.browser.document
import kotlinx.dom.appendText
import kotlinx.dom.clear
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import world.phantasmal.core.disposable.Scope
import world.phantasmal.core.disposable.disposable
import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.list.ListValChangeEvent

fun <E : Event> disposableListener(
    scope: Scope,
    target: EventTarget,
    type: String,
    listener: (E) -> Unit,
    options: AddEventListenerOptions? = null,
) {
    @Suppress("UNCHECKED_CAST")
    target.addEventListener(type, listener as (Event) -> Unit, options)

    scope.disposable {
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
    scope: Scope,
    list: ListVal<T>,
    createChild: (T, Int) -> Node,
) {
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

    list.observeList(scope) { change: ListValChangeEvent<T> ->
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

    scope.disposable { clear() }
}
