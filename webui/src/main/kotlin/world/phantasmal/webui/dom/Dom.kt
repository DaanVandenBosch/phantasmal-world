package world.phantasmal.webui.dom

import org.w3c.dom.AddEventListenerOptions
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
