package world.phantasmal.webui.dom

import kotlinx.browser.document
import kotlinx.dom.appendText
import org.w3c.dom.AddEventListenerOptions
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.Node
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.disposable

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

fun Node.root(className: String? = null, block: HTMLElement.() -> Unit): HTMLElement {
    val styleEl = document.createElement("style") as HTMLStyleElement
    styleEl.id = "pw-root-styles"
    styleEl.appendText(DEFAULT_STYLE)
    document.head!!.append(styleEl)

    return div(id = "pw-root", className, block = block)
}
