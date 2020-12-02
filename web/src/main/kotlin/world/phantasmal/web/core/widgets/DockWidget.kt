package world.phantasmal.web.core.widgets

import kotlinx.coroutines.launch
import org.w3c.dom.Node
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.trueVal
import world.phantasmal.web.core.controllers.*
import world.phantasmal.web.externals.goldenLayout.GoldenLayout
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.obj
import world.phantasmal.webui.widgets.Widget

class DockWidget(
    visible: Val<Boolean> = trueVal(),
    private val ctrl: DockController,
    private val createWidget: (id: String) -> Widget?,
) : Widget(visible) {
    private var goldenLayout: GoldenLayout? = null

    init {
        js("""require("golden-layout/src/css/goldenlayout-base.css");""")
    }

    override fun Node.createElement() =
        div {
            className = "pw-core-dock"

            val outerElement = this

            scope.launch {
                val dockedWidgetIds = mutableSetOf<String>()

                val config = createConfig(ctrl.initialConfig(), dockedWidgetIds)

                if (disposed) return@launch

                if (outerElement.offsetWidth == 0 || outerElement.offsetHeight == 0) {
                    // Temporarily set width and height so GoldenLayout initializes correctly.
                    style.width = "1000px"
                    style.height = "700px"
                }

                val goldenLayout = GoldenLayout(config, outerElement)
                this@DockWidget.goldenLayout = goldenLayout

                dockedWidgetIds.forEach { id ->
                    goldenLayout.registerComponent(id) { container: GoldenLayout.Container ->
                        val node = container.getElement()[0] as Node

                        createWidget(id)?.let { widget ->
                            node.addChild(widget)
                            widget.focus()
                        }
                    }
                }

                goldenLayout.on<Any>("stateChanged", {
                    val content = goldenLayout.toConfig().content

                    if (content is Array<*> && content.length > 0) {
                        fromGoldenLayoutConfig(
                            content.unsafeCast<Array<GoldenLayout.ItemConfig>>().first(),
                            useWidthAsFlex = null,
                        )?.let {
                            scope.launch { ctrl.configChanged(it) }
                        }
                    }
                })

                goldenLayout.init()

                style.width = ""
                style.height = ""

                addDisposable(size.observe { (size) ->
                    goldenLayout.updateSize(size.width, size.height)
                })
            }
        }

    override fun internalDispose() {
        goldenLayout?.destroy()
        super.internalDispose()
    }

    companion object {
        private const val HEADER_HEIGHT = 24
        private const val DEFAULT_HEADER_HEIGHT = 20

        /**
         * This value is used to work around a bug in GoldenLayout related to headerHeight.
         */
        private const val HEADER_HEIGHT_DIFF = HEADER_HEIGHT - DEFAULT_HEADER_HEIGHT

        private fun createConfig(
            item: DockedItem,
            dockedWidgetIds: MutableSet<String>,
        ): GoldenLayout.Config =
            obj {
                settings = obj<GoldenLayout.Settings> {
                    showPopoutIcon = false
                    showMaximiseIcon = false
                    showCloseIcon = false
                }
                dimensions = obj<GoldenLayout.Dimensions> {
                    headerHeight = HEADER_HEIGHT
                }
                content = arrayOf(
                    toGoldenLayoutConfig(item, dockedWidgetIds)
                )
            }

        private fun toGoldenLayoutConfig(
            item: DockedItem,
            dockedWidgetIds: MutableSet<String>,
        ): GoldenLayout.ItemConfig {
            val itemType = when (item) {
                is DockedRow -> "row"
                is DockedColumn -> "column"
                is DockedStack -> "stack"
                is DockedWidget -> "component"
            }

            return when (item) {
                is DockedWidget -> {
                    dockedWidgetIds.add(item.id)

                    obj<GoldenLayout.ComponentConfig> {
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
                    obj {
                        type = itemType
                        content = Array(item.items.size) {
                            toGoldenLayoutConfig(item.items[it], dockedWidgetIds)
                        }

                        if (item.flex != null) {
                            width = item.flex
                            height = item.flex
                        }

                        if (item is DockedStack) {
                            activeItemIndex = item.activeItemIndex
                        }
                    }
            }
        }

        private fun fromGoldenLayoutConfig(
            item: GoldenLayout.ItemConfig,
            useWidthAsFlex: Boolean?,
        ): DockedItem? {
            val flex = when (useWidthAsFlex) {
                true -> item.width
                false -> item.height
                null -> null
            }

            return when (item.type) {
                "row" -> DockedRow(
                    flex,
                    items = item.content
                        ?.mapNotNull { fromGoldenLayoutConfig(it, useWidthAsFlex = true) }
                        ?: emptyList()
                )

                "column" -> DockedColumn(
                    flex,
                    items = item.content
                        ?.mapNotNull { fromGoldenLayoutConfig(it, useWidthAsFlex = false) }
                        ?: emptyList()
                )

                "stack" -> {
                    DockedStack(
                        item.activeItemIndex,
                        flex,
                        items = item.content
                            ?.mapNotNull { fromGoldenLayoutConfig(it, useWidthAsFlex = null) }
                            ?: emptyList()
                    )
                }

                "component" -> {
                    val id =
                        (item.unsafeCast<GoldenLayout.ComponentConfig>()).componentName as String?
                    val title = item.title

                    if (id == null || title == null) {
                        null
                    } else {
                        DockedWidget(id, title, flex)
                    }
                }

                else -> null
            }
        }

        init {
            // Use #pw-root for higher specificity than the default GoldenLayout CSS.
            @Suppress("CssUnusedSymbol", "CssUnresolvedCustomProperty")
            // language=css
            style("""
                .pw-core-dock {
                    width: 100%;
                    height: 100%;
                    overflow: hidden;
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
            """.trimIndent())
        }
    }
}
