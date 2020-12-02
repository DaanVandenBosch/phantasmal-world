package world.phantasmal.web.application.widgets

import org.w3c.dom.Node
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.Widget

class ApplicationWidget(
    private val navigationWidget: NavigationWidget,
    private val mainContentWidget: MainContentWidget,
) : Widget() {
    override fun Node.createElement() =
        div {
            className = "pw-application-application"

            addChild(navigationWidget)
            addChild(mainContentWidget)
        }

    companion object {
        init {
            @Suppress("CssUnusedSymbol")
            // language=css
            style("""
                .pw-application-application {
                    position: fixed;
                    top: 0;
                    bottom: 0;
                    left: 0;
                    right: 0;
                    display: flex;
                    flex-direction: column;
                }
                .pw-application-application .pw-application-main-content {
                    flex-grow: 1;
                    overflow: hidden;
                }
            """.trimIndent())
        }
    }
}
