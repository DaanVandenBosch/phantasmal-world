package world.phantasmal.webui.dom

import kotlinx.browser.document
import org.w3c.dom.*

fun Node.a(block: HTMLAnchorElement.() -> Unit = {}): HTMLAnchorElement =
    appendHtmlEl("A", block)

fun Node.button(block: HTMLButtonElement.() -> Unit = {}): HTMLButtonElement =
    appendHtmlEl("BUTTON", block)

fun Node.canvas(block: HTMLCanvasElement.() -> Unit = {}): HTMLCanvasElement =
    appendHtmlEl("CANVAS", block)

fun Node.div(block: HTMLDivElement .() -> Unit = {}): HTMLDivElement =
    appendHtmlEl("DIV", block)

fun Node.form(block: HTMLFormElement.() -> Unit = {}): HTMLFormElement =
    appendHtmlEl("FORM", block)

fun Node.h1(block: HTMLHeadingElement.() -> Unit = {}): HTMLHeadingElement =
    appendHtmlEl("H1", block)

fun Node.h2(block: HTMLHeadingElement.() -> Unit = {}): HTMLHeadingElement =
    appendHtmlEl("H2", block)

fun Node.header(block: HTMLElement.() -> Unit = {}): HTMLElement =
    appendHtmlEl("HEADER", block)

fun Node.img(block: HTMLImageElement.() -> Unit = {}): HTMLImageElement =
    appendHtmlEl("IMG", block)

fun Node.input(block: HTMLInputElement.() -> Unit = {}): HTMLInputElement =
    appendHtmlEl("INPUT", block)

fun Node.label(block: HTMLLabelElement.() -> Unit = {}): HTMLLabelElement =
    appendHtmlEl("LABEL", block)

fun Node.main(block: HTMLElement.() -> Unit = {}): HTMLElement =
    appendHtmlEl("MAIN", block)

fun Node.p(block: HTMLParagraphElement.() -> Unit = {}): HTMLParagraphElement =
    appendHtmlEl("P", block)

fun Node.span(block: HTMLSpanElement.() -> Unit = {}): HTMLSpanElement =
    appendHtmlEl("SPAN", block)

fun Node.table(block: HTMLTableElement.() -> Unit = {}): HTMLTableElement =
    appendHtmlEl("TABLE", block)

fun Node.td(block: HTMLTableCellElement.() -> Unit = {}): HTMLTableCellElement =
    appendHtmlEl("TD", block)

fun Node.textarea(block: HTMLTextAreaElement.() -> Unit = {}): HTMLTextAreaElement =
    appendHtmlEl("TEXTAREA", block)

fun Node.th(block: HTMLTableCellElement.() -> Unit = {}): HTMLTableCellElement =
    appendHtmlEl("TH", block)

fun Node.tr(block: HTMLTableRowElement.() -> Unit = {}): HTMLTableRowElement =
    appendHtmlEl("TR", block)

fun <T : HTMLElement> Node.appendHtmlEl(tagName: String, block: T.() -> Unit): T =
    appendChild(newHtmlEl(tagName, block)).unsafeCast<T>()

fun <T : HTMLElement> newHtmlEl(tagName: String, block: T.() -> Unit): T =
    newEl(tagName, block)

private fun <T : Element> newEl(tagName: String, block: T.() -> Unit): T {
    val el = document.createElement(tagName).unsafeCast<T>()
    el.block()
    return el
}
