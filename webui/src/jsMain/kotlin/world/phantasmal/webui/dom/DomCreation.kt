package world.phantasmal.webui.dom

import kotlinx.browser.document
import org.w3c.dom.*
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract

inline fun <T : Node> dom(block: Node.() -> T): T {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return documentFragment().block()
}

@Suppress("NOTHING_TO_INLINE")
inline fun documentFragment(): DocumentFragment =
    document.createDocumentFragment()

inline fun Node.button(block: HTMLButtonElement.() -> Unit = {}): HTMLButtonElement {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return appendHtmlEl("BUTTON", block)
}

inline fun Node.canvas(block: HTMLCanvasElement.() -> Unit = {}): HTMLCanvasElement {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return appendHtmlEl("CANVAS", block)
}

inline fun Node.div(block: HTMLDivElement .() -> Unit = {}): HTMLDivElement {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return appendHtmlEl("DIV", block)
}

inline fun Node.form(block: HTMLFormElement.() -> Unit = {}): HTMLFormElement {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return appendHtmlEl("FORM", block)
}

inline fun Node.h1(block: HTMLHeadingElement.() -> Unit = {}): HTMLHeadingElement {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return appendHtmlEl("H1", block)
}

inline fun Node.h2(block: HTMLHeadingElement.() -> Unit = {}): HTMLHeadingElement {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return appendHtmlEl("H2", block)
}

inline fun Node.h3(block: HTMLHeadingElement.() -> Unit = {}): HTMLHeadingElement {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return appendHtmlEl("H3", block)
}

inline fun Node.h4(block: HTMLHeadingElement.() -> Unit = {}): HTMLHeadingElement {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return appendHtmlEl("H4", block)
}

inline fun Node.h5(block: HTMLHeadingElement.() -> Unit = {}): HTMLHeadingElement {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return appendHtmlEl("H5", block)
}

inline fun Node.header(block: HTMLElement.() -> Unit = {}): HTMLElement {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return appendHtmlEl("HEADER", block)
}

inline fun Node.img(block: HTMLImageElement.() -> Unit = {}): HTMLImageElement {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return appendHtmlEl("IMG", block)
}

inline fun Node.input(block: HTMLInputElement.() -> Unit = {}): HTMLInputElement {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return appendHtmlEl("INPUT", block)
}

inline fun Node.label(block: HTMLLabelElement.() -> Unit = {}): HTMLLabelElement {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return appendHtmlEl("LABEL", block)
}

inline fun Node.li(block: HTMLLIElement.() -> Unit = {}): HTMLLIElement {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return appendHtmlEl("LI", block)
}

inline fun Node.main(block: HTMLElement.() -> Unit = {}): HTMLElement {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return appendHtmlEl("MAIN", block)
}

inline fun Node.p(block: HTMLParagraphElement.() -> Unit = {}): HTMLParagraphElement {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return appendHtmlEl("P", block)
}

inline fun Node.section(block: HTMLElement.() -> Unit = {}): HTMLElement {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return appendHtmlEl("SECTION", block)
}

inline fun Node.span(block: HTMLSpanElement.() -> Unit = {}): HTMLSpanElement {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return appendHtmlEl("SPAN", block)
}

inline fun Node.table(block: HTMLTableElement.() -> Unit = {}): HTMLTableElement {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return appendHtmlEl("TABLE", block)
}

inline fun Node.tbody(block: HTMLTableSectionElement.() -> Unit = {}): HTMLTableSectionElement {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return appendHtmlEl("TBODY", block)
}

inline fun Node.td(block: HTMLTableCellElement.() -> Unit = {}): HTMLTableCellElement {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return appendHtmlEl("TD", block)
}

inline fun Node.textarea(block: HTMLTextAreaElement.() -> Unit = {}): HTMLTextAreaElement {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return appendHtmlEl("TEXTAREA", block)
}

inline fun Node.tfoot(block: HTMLTableSectionElement.() -> Unit = {}): HTMLTableSectionElement {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return appendHtmlEl("TFOOT", block)
}

inline fun Node.th(block: HTMLTableCellElement.() -> Unit = {}): HTMLTableCellElement {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return appendHtmlEl("TH", block)
}

inline fun Node.thead(block: HTMLTableSectionElement.() -> Unit = {}): HTMLTableSectionElement {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return appendHtmlEl("THEAD", block)
}

inline fun Node.tr(block: HTMLTableRowElement.() -> Unit = {}): HTMLTableRowElement {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return appendHtmlEl("TR", block)
}

inline fun Node.ul(block: HTMLUListElement.() -> Unit = {}): HTMLUListElement {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return appendHtmlEl("UL", block)
}

inline fun <T : HTMLElement> Node.appendHtmlEl(tagName: String, block: T.() -> Unit): T {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return appendChild(newHtmlEl(tagName, block)).unsafeCast<T>()
}

inline fun <T : HTMLElement> newHtmlEl(tagName: String, block: T.() -> Unit): T {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    return newEl(tagName, block)
}

inline fun <T : Element> newEl(tagName: String, block: T.() -> Unit): T {
    contract { callsInPlace(block, EXACTLY_ONCE) }
    val el = document.createElement(tagName).unsafeCast<T>()
    el.block()
    return el
}
