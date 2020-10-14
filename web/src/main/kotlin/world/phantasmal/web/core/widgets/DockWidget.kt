package world.phantasmal.web.core.widgets

import org.w3c.dom.Node
import world.phantasmal.core.disposable.Scope
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal
import world.phantasmal.web.core.newJsObject
import world.phantasmal.web.externals.GoldenLayout
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.widgets.Widget

private const val HEADER_HEIGHT = 24
private const val DEFAULT_HEADER_HEIGHT = 20

/**
 * This value is used to work around a bug in GoldenLayout related to headerHeight.
 */
private const val HEADER_HEIGHT_DIFF = HEADER_HEIGHT - DEFAULT_HEADER_HEIGHT

sealed class DockedItem(val flex: Int?)
sealed class DockedContainer(flex: Int?, val items: List<DockedItem>) : DockedItem(flex)

class DockedRow(
    flex: Int? = null,
    items: List<DockedItem> = emptyList(),
) : DockedContainer(flex, items)

class DockedColumn(
    flex: Int? = null,
    items: List<DockedItem> = emptyList(),
) : DockedContainer(flex, items)

class DockedStack(
    flex: Int? = null,
    items: List<DockedItem> = emptyList(),
) : DockedContainer(flex, items)

class DocketWidget(
    val id: String,
    val title: String,
    flex: Int? = null,
    val createWidget: (Scope) -> Widget,
) : DockedItem(flex)

class DockWidget(
    scope: Scope,
    hidden: Val<Boolean> = falseVal(),
    private val item: DockedItem,
) : Widget(scope, ::style, hidden) {
    private lateinit var goldenLayout: GoldenLayout

    init {
        try {
            // Importing the base CSS fails during unit tests.
            js("""require("golden-layout/src/css/goldenlayout-base.css");""")
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        observeResize()
    }

    override fun Node.createElement() = div(className = "pw-core-dock") {
        val idToCreate = mutableMapOf<String, (Scope) -> Widget>()

        val config = newJsObject<GoldenLayout.Config> {
            settings = newJsObject<GoldenLayout.Settings> {
                showPopoutIcon = false
                showMaximiseIcon = false
                showCloseIcon = false
            }
            dimensions = newJsObject<GoldenLayout.Dimensions> {
                headerHeight = HEADER_HEIGHT
            }
            content = arrayOf(
                toConfigContent(item, idToCreate)
            )
        }

        // Temporarily set width and height so GoldenLayout initializes correctly.
        style.width = "1000px"
        style.height = "700px"

        goldenLayout = GoldenLayout(config, this)

        idToCreate.forEach { (id, create) ->
            goldenLayout.registerComponent(id) { container: GoldenLayout.Container ->
                container.getElement().append(create(scope).element)
            }
        }

        goldenLayout.init()

        style.width = ""
        style.height = ""
    }

    override fun resized(width: Double, height: Double) {
        goldenLayout.updateSize(width, height)
    }

    override fun internalDispose() {
        goldenLayout.destroy()
        super.internalDispose()
    }

    private fun toConfigContent(
        item: DockedItem,
        idToCreate: MutableMap<String, (Scope) -> Widget>,
    ): GoldenLayout.ItemConfig {
        val itemType = when (item) {
            is DockedRow -> "row"
            is DockedColumn -> "column"
            is DockedStack -> "stack"
            is DocketWidget -> "component"
        }

        return when (item) {
            is DocketWidget -> {
                idToCreate[item.id] = item.createWidget

                newJsObject<GoldenLayout.ComponentConfig> {
                    title = item.title
                    type = "component"
                    componentName = item.id
                    isClosable = false

                    if (item.flex != null) {
                        width = item.flex
                        height = item.flex
                    }
                }
            }

            is DockedContainer ->
                newJsObject {
                    type = itemType
                    content = Array(item.items.size) { toConfigContent(item.items[it], idToCreate) }

                    if (item.flex != null) {
                        width = item.flex
                        height = item.flex
                    }
                }
        }
    }
}

// Use #pw-root for higher specificity than the default GoldenLayout CSS.
@Suppress("CssUnusedSymbol", "CssUnresolvedCustomProperty")
// language=css
private fun style() = """
.pw-core-dock {
    width: 100%;
    height: 100%;
}

#pw-root .lm_header {
    box-sizing: border-box;
    height: ${HEADER_HEIGHT + 4}px;
    padding: 3px 0 0 0;
    border-bottom: var(--pw-border);
}

#pw-root .lm_header .lm_tabs {
    padding: 0 3px;
}

#pw-root .lm_header .lm_tabs .lm_tab {
    cursor: default;
    display: inline-flex;
    align-items: center;
    height: 23px;
    padding: 0 10px;
    border: var(--pw-border);
    margin: 0 1px -1px 1px;
    background-color: hsl(0, 0%, 12%);
    color: hsl(0, 0%, 75%);
    font-size: 13px;
}

#pw-root .lm_header .lm_tabs .lm_tab:hover {
    background-color: hsl(0, 0%, 18%);
    color: hsl(0, 0%, 85%);
}

#pw-root .lm_header .lm_tabs .lm_tab.lm_active {
    background-color: var(--pw-bg-color);
    color: hsl(0, 0%, 90%);
    border-bottom-color: var(--pw-bg-color);
}

#pw-root .lm_header .lm_controls > li {
    cursor: default;
}

#pw-root .lm_header .lm_controls .lm_close {
    /* a white 9x9 X shape */
    background-image: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAkAAAAJCAYAAADgkQYQAAAAQUlEQVR4nHXOQQ4AMAgCQeT/f6aXpsGK3jSTuCVJAAr7iBdoAwCKd0nwfaAdHbYERw5b44+E8JoBjEYGMBq5gAYP3usUDu2IvoUAAAAASUVORK5CYII=);
    background-position: center center;
    background-repeat: no-repeat;
    cursor: pointer;
    opacity: 0.4;
    transition: opacity 300ms ease;
}

#pw-root .lm_header .lm_controls .lm_close:hover {
    opacity: 1;
}

#pw-root .lm_content > * {
    width: 100%;
    /* Subtract HEADER_HEIGHT_DIFF px as workaround for bug related to headerHeight. */
    height: calc(100% - ${HEADER_HEIGHT_DIFF}px);
}

#pw-root .lm_splitter {
    box-sizing: border-box;
    background-color: hsl(0, 0%, 20%);
}

#pw-root .lm_splitter.lm_vertical {
    border-top: var(--pw-border);
    border-bottom: var(--pw-border);
}

#pw-root .lm_splitter.lm_horizontal {
    border-left: var(--pw-border);
    border-right: var(--pw-border);
}

#pw-root .lm_dragProxy > .lm_content {
    box-sizing: border-box;
    background-color: var(--pw-bg-color);
    border-left: var(--pw-border);
    border-right: var(--pw-border);
    border-bottom: var(--pw-border);
}

#pw-root .lm_dropTargetIndicator {
    box-sizing: border-box;
    background-color: hsla(0, 0%, 50%, 0.2);
}
"""
