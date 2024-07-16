package world.phantasmal.web.core.dom

import kotlinx.browser.window
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import world.phantasmal.web.shared.dto.SectionId
import world.phantasmal.webui.dom.appendHtmlEl
import world.phantasmal.webui.dom.span

private val ASSET_BASE_PATH: String = window.location.pathname.removeSuffix("/") + "/assets"

fun Node.externalLink(href: String, block: HTMLAnchorElement.() -> Unit) =
    appendHtmlEl<HTMLAnchorElement>("A") {
        target = "_blank"
        rel = "noopener noreferrer"
        this.href = href
        block()
    }

fun Node.sectionIdIcon(sectionId: SectionId, size: Int): HTMLElement =
    span {
        style.display = "inline-block"
        style.width = "${size}px"
        style.height = "${size}px"
        style.backgroundImage = "url($ASSET_BASE_PATH/images/sectionids/${sectionId}.png)"
        style.backgroundSize = "${size}px"
        title = sectionId.name
    }
