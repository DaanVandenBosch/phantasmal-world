package world.phantasmal.web.core.dom

import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.Node
import world.phantasmal.webui.dom.appendHtmlEl

fun Node.externalLink(href: String, block: HTMLAnchorElement.() -> Unit) =
    appendHtmlEl<HTMLAnchorElement>("A") {
        target = "_blank"
        rel = "noopener noreferrer"
        this.href = href
        block()
    }
