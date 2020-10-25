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
