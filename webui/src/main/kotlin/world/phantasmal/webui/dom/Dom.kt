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

fun HTMLElement.root(): HTMLElement {
    val styleEl = document.createElement("style") as HTMLStyleElement
    styleEl.id = "pw-root-styles"
    styleEl.appendText(DEFAULT_STYLE)
    document.head!!.append(styleEl)

    id = "pw-root"
    return this
}

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
