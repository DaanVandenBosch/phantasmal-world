package world.phantasmal.webui.dom

import kotlinx.browser.document
import org.w3c.dom.*

fun Node.a(
    href: String? = null,
    id: String? = null,
    className: String? = null,
    title: String? = null,
    tabIndex: Int? = null,
    block: HTMLAnchorElement.() -> Unit = {},
): HTMLAnchorElement =
    appendHtmlEl("A", id, className, title, tabIndex) {
        if (href != null) this.href = href
        block()
    }

fun Node.button(
    type: String? = null,
    id: String? = null,
    className: String? = null,
    title: String? = null,
    tabIndex: Int? = null,
    block: HTMLButtonElement.() -> Unit = {},
): HTMLButtonElement =
    appendHtmlEl("BUTTON", id, className, title, tabIndex) {
        if (type != null) this.type = type
        block()
    }

fun Node.canvas(
    id: String? = null,
    className: String? = null,
    title: String? = null,
    tabIndex: Int? = null,
    block: HTMLCanvasElement.() -> Unit = {},
): HTMLCanvasElement =
    appendHtmlEl("CANVAS", id, className, title, tabIndex, block)

fun Node.div(
    id: String? = null,
    className: String? = null,
    title: String? = null,
    tabIndex: Int? = null,
    block: HTMLDivElement .() -> Unit = {},
): HTMLDivElement =
    appendHtmlEl("DIV", id, className, title, tabIndex, block)

fun Node.form(
    id: String? = null,
    className: String? = null,
    title: String? = null,
    tabIndex: Int? = null,
    block: HTMLFormElement.() -> Unit = {},
): HTMLFormElement =
    appendHtmlEl("FORM", id, className, title, tabIndex, block)

fun Node.h1(
    id: String? = null,
    className: String? = null,
    title: String? = null,
    tabIndex: Int? = null,
    block: HTMLHeadingElement.() -> Unit = {},
): HTMLHeadingElement =
    appendHtmlEl("H1", id, className, title, tabIndex, block)

fun Node.h2(
    id: String? = null,
    className: String? = null,
    title: String? = null,
    tabIndex: Int? = null,
    block: HTMLHeadingElement.() -> Unit = {},
): HTMLHeadingElement =
    appendHtmlEl("H2", id, className, title, tabIndex, block)

fun Node.header(
    id: String? = null,
    className: String? = null,
    title: String? = null,
    tabIndex: Int? = null,
    block: HTMLElement.() -> Unit = {},
): HTMLElement =
    appendHtmlEl("HEADER", id, className, title, tabIndex, block)

fun Node.img(
    src: String? = null,
    width: Int? = null,
    height: Int? = null,
    alt: String? = null,
    id: String? = null,
    className: String? = null,
    title: String? = null,
    tabIndex: Int? = null,
    block: HTMLImageElement.() -> Unit = {},
): HTMLImageElement =
    appendHtmlEl("IMG", id, className, title, tabIndex) {
        if (src != null) this.src = src
        if (width != null) this.width = width
        if (height != null) this.height = height
        if (alt != null) this.alt = alt
        block()
    }

fun Node.input(
    type: String? = null,
    id: String? = null,
    className: String? = null,
    title: String? = null,
    tabIndex: Int? = null,
    block: HTMLInputElement.() -> Unit = {},
): HTMLInputElement =
    appendHtmlEl("INPUT", id, className, title, tabIndex) {
        if (type != null) this.type = type
        block()
    }

fun Node.label(
    htmlFor: String? = null,
    id: String? = null,
    className: String? = null,
    title: String? = null,
    tabIndex: Int? = null,
    block: HTMLLabelElement.() -> Unit = {},
): HTMLLabelElement =
    appendHtmlEl("LABEL", id, className, title, tabIndex) {
        if (htmlFor != null) this.htmlFor = htmlFor
        block()
    }

fun Node.main(
    id: String? = null,
    className: String? = null,
    title: String? = null,
    tabIndex: Int? = null,
    block: HTMLElement.() -> Unit = {},
): HTMLElement =
    appendHtmlEl("MAIN", id, className, title, tabIndex, block)

fun Node.p(
    id: String? = null,
    className: String? = null,
    title: String? = null,
    tabIndex: Int? = null,
    block: HTMLParagraphElement.() -> Unit = {},
): HTMLParagraphElement =
    appendHtmlEl("P", id, className, title, tabIndex, block)

fun Node.span(
    id: String? = null,
    className: String? = null,
    title: String? = null,
    tabIndex: Int? = null,
    block: HTMLSpanElement.() -> Unit = {},
): HTMLSpanElement =
    appendHtmlEl("SPAN", id, className, title, tabIndex, block)

fun Node.table(
    id: String? = null,
    className: String? = null,
    title: String? = null,
    tabIndex: Int? = null,
    block: HTMLTableElement.() -> Unit = {},
): HTMLTableElement =
    appendHtmlEl("TABLE", id, className, title, tabIndex, block)

fun Node.td(
    id: String? = null,
    className: String? = null,
    title: String? = null,
    tabIndex: Int? = null,
    block: HTMLTableCellElement.() -> Unit = {},
): HTMLTableCellElement =
    appendHtmlEl("TD", id, className, title, tabIndex, block)

fun Node.th(
    id: String? = null,
    className: String? = null,
    title: String? = null,
    tabIndex: Int? = null,
    block: HTMLTableCellElement.() -> Unit = {},
): HTMLTableCellElement =
    appendHtmlEl("TH", id, className, title, tabIndex, block)

fun Node.tr(
    id: String? = null,
    className: String? = null,
    title: String? = null,
    tabIndex: Int? = null,
    block: HTMLTableRowElement.() -> Unit = {},
): HTMLTableRowElement =
    appendHtmlEl("TR", id, className, title, tabIndex, block)

fun <T : HTMLElement> Node.appendHtmlEl(
    tagName: String,
    id: String? = null,
    className: String? = null,
    title: String? = null,
    tabIndex: Int? = null,
    block: T.() -> Unit,
): T =
    appendChild(newHtmlEl(tagName, id, className, title, tabIndex, block)).unsafeCast<T>()

fun <T : HTMLElement> newHtmlEl(
    tagName: String,
    id: String? = null,
    className: String? = null,
    title: String? = null,
    tabIndex: Int? = null,
    block: T.() -> Unit,
): T =
    newEl(tagName, id, className) {
        if (title != null) this.title = title
        if (tabIndex != null) this.tabIndex = tabIndex
        block()
    }

private fun <T : Element> newEl(
    tagName: String,
    id: String? = null,
    className: String?,
    block: T.() -> Unit,
): T {
    val el = document.createElement(tagName).unsafeCast<T>()
    if (id != null) el.id = id
    if (className != null) el.className = className
    el.block()
    return el
}
