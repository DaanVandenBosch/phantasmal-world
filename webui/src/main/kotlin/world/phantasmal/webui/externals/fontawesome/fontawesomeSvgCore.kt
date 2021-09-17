@file:JsModule("@fortawesome/fontawesome-svg-core")
@file:JsNonModule

package world.phantasmal.webui.externals.fontawesome

import org.w3c.dom.HTMLCollection

external class IconDefinition

external class Icon {
    val html: Array<String>
    val node: HTMLCollection
}

external fun icon(icon: IconDefinition): Icon

external class Config {
    /** async or sync */
    var mutateApproach: String
}

external val config: Config
