package world.phantasmal.web.core.widgets

import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.w3c.dom.Node
import world.phantasmal.cell.Cell
import world.phantasmal.cell.trueCell
import world.phantasmal.web.core.controllers.*
import world.phantasmal.web.externals.goldenLayout.GoldenLayout
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.obj
import world.phantasmal.webui.widgets.Widget

private val logger = KotlinLogging.logger {}

class DockWidget(
    visible: Cell<Boolean> = trueCell(),
    private val ctrl: DockController,
    private val createWidget: (id: String) -> Widget,
) : Widget(visible) {
    private var goldenLayout: GoldenLayout? = null
    private val idToChildWidget = mutableMapOf<String, Widget>()

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
                    // registerComponent expects a regular function and not an arrow function. This
                    // function will be called with new.
                    goldenLayout.registerComponent(id) { container: GoldenLayout.Container ->
                        createChildWidget(id, container)
                    }
                }

                goldenLayout.on<Any>("stateChanged", { onStateChanged() })

                goldenLayout.on("stackCreated", ::onStackCreated)

                goldenLayout.init()

                style.width = ""
                style.height = ""

                addDisposable(size.observeChange { (size) ->
                    goldenLayout.updateSize(size.width, size.height)
                })
            }
        }

    override fun dispose() {
        goldenLayout?.destroy()
        super.dispose()
    }

    private fun createChildWidget(id: String, container: GoldenLayout.Container) {
        val node = container.getElement()[0] as Node

        val widget =
            try {
                idToChildWidget[id]?.let { prevWidget ->
                    logger.error { """Widget with ID "$id" was already created.""" }
                    prevWidget.dispose()
                }

                node.addChild(createWidget(id)).apply {
                    focus()
                }
            } catch (e: Exception) {
                logger.error(e) { """Couldn't instantiate widget with ID "$id".""" }

                node.addChild(
                    UnavailableWidget(
                        message = "Something went wrong while initializing this tab.",
                    )
                )
            }

        idToChildWidget[id] = widget

        container.on("close", {
            removeChild(widget)
            idToChildWidget.remove(id)
        })
    }

    private fun onStateChanged() {
        val content = goldenLayout?.toConfig()?.content

        if (content is Array<*> && content.length > 0) {
            fromGoldenLayoutConfig(
                content.unsafeCast<Array<GoldenLayout.ItemConfig>>().first(),
                useWidthAsFlex = null,
            )?.let {
                scope.launch { ctrl.configChanged(it) }
            }
        }
    }

    private fun onStackCreated(stack: GoldenLayout.ContentItem) {
        stack.on("activeContentItemChanged", { item: GoldenLayout.ContentItem ->
            val config = item.config.unsafeCast<GoldenLayout.ComponentConfig>()

            if (config.componentName != undefined) {
                idToChildWidget[config.componentName]?.focus()
            }
        })
    }

    companion object {
        private const val HEADER_HEIGHT = 22

        /**
         * This value is used to work around a bug in GoldenLayout related to headerHeight.
         */
        private const val HEADER_HEIGHT_DIFF = 4

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
                    require(dockedWidgetIds.add(item.id)) {
                        """ID ${item.id} is used more than once."""
                    }

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
                        (item.unsafeCast<GoldenLayout.ComponentConfig>()).componentName
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
            @Suppress("CssUnusedSymbol", "CssUnresolvedCustomProperty", "CssInvalidPropertyValue")
            // language=css
            style(
                """
                .pw-core-dock {
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
                    height: ${HEADER_HEIGHT - 1}px;
                    padding: 0 8px;
                    border: var(--pw-border);
                    margin: 0 1px -1px 1px;
                    background-color: hsl(0, 0%, 12%);
                    color: hsl(0, 0%, 75%);
                    font-size: 12px;
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
                """.trimIndent()
            )
        }
    }
}
