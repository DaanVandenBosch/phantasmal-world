@file:Suppress("unused")

package world.phantasmal.web.externals.goldenLayout

import org.w3c.dom.Element

@JsModule("golden-layout")
@JsNonModule
external class GoldenLayout(configuration: Config, container: Element = definedExternally) {
    fun init()
    fun updateSize(width: Double, height: Double)
    fun registerComponent(name: String, component: Any)
    fun destroy()
    fun <E> on(eventName: String, callback: (E) -> Unit, context: Any = definedExternally)
    fun toConfig(): dynamic

    interface Settings {
        var hasHeaders: Boolean?
        var constrainDragToContainer: Boolean?
        var reorderEnabled: Boolean?
        var selectionEnabled: Boolean?
        var popoutWholeStack: Boolean?
        var blockedPopoutsThrowError: Boolean?
        var closePopoutsOnUnload: Boolean?
        var showPopoutIcon: Boolean?
        var showMaximiseIcon: Boolean?
        var showCloseIcon: Boolean?
    }

    interface Dimensions {
        var borderWidth: Number?
        var minItemHeight: Number?
        var minItemWidth: Number?
        var headerHeight: Number?
        var dragProxyWidth: Number?
        var dragProxyHeight: Number?
    }

    interface Labels {
        var close: String?
        var maximise: String?
        var minimise: String?
        var popout: String?
    }

    interface ItemConfig {
        var type: String
        var content: Array<ItemConfig>?
        var width: Double?
        var height: Double?
        var id: String? /* String? | Array<String>? */
        var isClosable: Boolean?
        var title: String?
        var activeItemIndex: Int?
    }

    interface ComponentConfig : ItemConfig {
        var componentName: String
        var componentState: Any?
    }

    interface ReactComponentConfig : ItemConfig {
        var component: String
        var props: Any?
    }

    interface Config {
        var settings: Settings?
        var dimensions: Dimensions?
        var labels: Labels?
        var content: Array<ItemConfig>?
    }

    interface ContentItem : EventEmitter {
        var config: ItemConfig
        var type: String
        var contentItems: Array<ContentItem>
        var parent: ContentItem
        var id: String
        var isInitialised: Boolean
        var isMaximised: Boolean
        var isRoot: Boolean
        var isRow: Boolean
        var isColumn: Boolean
        var isStack: Boolean
        var isComponent: Boolean
        var layoutManager: Any
        var element: Container
        var childElementContainer: Container
        fun addChild(itemOrItemConfig: ContentItem, index: Number = definedExternally)
        fun addChild(itemOrItemConfig: ItemConfig, index: Number = definedExternally)
        fun addChild(itemOrItemConfig: ComponentConfig, index: Number = definedExternally)
        fun addChild(itemOrItemConfig: ReactComponentConfig, index: Number = definedExternally)
        fun removeChild(contentItem: Config, keepChild: Boolean = definedExternally)
        fun replaceChild(oldChild: ContentItem, newChild: ContentItem)
        fun replaceChild(oldChild: ContentItem, newChild: ItemConfig)
        fun replaceChild(oldChild: ContentItem, newChild: ComponentConfig)
        fun replaceChild(oldChild: ContentItem, newChild: ReactComponentConfig)
        fun setSize()
        fun setTitle(title: String)
        fun callDownwards(
            functionName: String,
            functionArguments: Array<Any> = definedExternally,
            bottomUp: Boolean = definedExternally,
            skipSelf: Boolean = definedExternally,
        )

        fun emitBubblingEvent(name: String)
        fun remove()
        fun toggleMaximise()
        fun select()
        fun deselect()
        fun hasId(id: String): Boolean
        fun setActiveContentItem(contentItem: ContentItem)
        fun getActiveContentItem(): ContentItem
        fun addId(id: String)
        fun removeId(id: String)
        fun getItemsByFilter(filterFunction: (contentItem: ContentItem) -> Boolean): Array<ContentItem>
        fun getItemsById(id: String): Array<ContentItem>
        fun getItemsById(id: Array<String>): Array<ContentItem>
        fun getItemsByType(type: String): Array<ContentItem>
        fun getComponentsByName(componentName: String): Any
    }

    interface Container : EventEmitter {
        var width: Number
        var height: Number
        var parent: ContentItem
        var tab: Tab
        var title: String
        var layoutManager: GoldenLayout
        var isHidden: Boolean
        fun setState(state: Any)
        fun extendState(state: Any)
        fun getState(): Any

        /**
         * Returns jQuery-wrapped element.
         */
        fun getElement(): dynamic
        fun hide(): Boolean
        fun show(): Boolean
        fun setSize(width: Number, height: Number): Boolean
        fun setTitle(title: String)
        fun close(): Boolean
    }

    interface Header {
        var layoutManager: GoldenLayout
        var parent: ContentItem
        var tabs: Array<Tab>
        var activeContentItem: ContentItem
        var element: Any
        var tabsContainer: Any
        var controlsContainer: Any
        fun setActiveContentItem(contentItem: ContentItem)
        fun createTab(contentItem: ContentItem, index: Number = definedExternally)
        fun removeTab(contentItem: ContentItem)
    }

    interface Tab {
        var isActive: Boolean
        var header: Header
        var contentItem: ContentItem
        var element: Any
        var titleElement: Any
        var closeElement: Any
        fun setTitle(title: String)
        fun setActive(isActive: Boolean)
    }

    interface EventEmitter {
        fun on(eventName: String, callback: Function<*>, context: Any = definedExternally)
        fun emit(
            eventName: String,
            arg1: Any = definedExternally,
            arg2: Any = definedExternally,
            vararg argN: Any,
        )

        fun trigger(
            eventName: String,
            arg1: Any = definedExternally,
            arg2: Any = definedExternally,
            vararg argN: Any,
        )

        fun unbind(
            eventName: String,
            callback: Function<*> = definedExternally,
            context: Any = definedExternally,
        )

        fun off(
            eventName: String,
            callback: Function<*> = definedExternally,
            context: Any = definedExternally,
        )
    }

    companion object {
        fun minifyConfig(config: Any): Any
        fun unminifyConfig(minifiedConfig: Any): Any
    }
}
