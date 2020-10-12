package golden_layout

import org.w3c.dom.Element

@JsModule("golden-layout")
@JsNonModule
external open class GoldenLayout(configuration: Config, container: Element = definedExternally) {
    open fun init()
    open fun updateSize(width: Double, height: Double)
    open fun registerComponent(name: String, component: Any)
    open fun destroy()

    interface Settings {
        var hasHeaders: Boolean?
            get() = definedExternally
            set(value) = definedExternally
        var constrainDragToContainer: Boolean?
            get() = definedExternally
            set(value) = definedExternally
        var reorderEnabled: Boolean?
            get() = definedExternally
            set(value) = definedExternally
        var selectionEnabled: Boolean?
            get() = definedExternally
            set(value) = definedExternally
        var popoutWholeStack: Boolean?
            get() = definedExternally
            set(value) = definedExternally
        var blockedPopoutsThrowError: Boolean?
            get() = definedExternally
            set(value) = definedExternally
        var closePopoutsOnUnload: Boolean?
            get() = definedExternally
            set(value) = definedExternally
        var showPopoutIcon: Boolean?
            get() = definedExternally
            set(value) = definedExternally
        var showMaximiseIcon: Boolean?
            get() = definedExternally
            set(value) = definedExternally
        var showCloseIcon: Boolean?
            get() = definedExternally
            set(value) = definedExternally
    }

    interface Dimensions {
        var borderWidth: Number?
            get() = definedExternally
            set(value) = definedExternally
        var minItemHeight: Number?
            get() = definedExternally
            set(value) = definedExternally
        var minItemWidth: Number?
            get() = definedExternally
            set(value) = definedExternally
        var headerHeight: Number?
            get() = definedExternally
            set(value) = definedExternally
        var dragProxyWidth: Number?
            get() = definedExternally
            set(value) = definedExternally
        var dragProxyHeight: Number?
            get() = definedExternally
            set(value) = definedExternally
    }

    interface Labels {
        var close: String?
            get() = definedExternally
            set(value) = definedExternally
        var maximise: String?
            get() = definedExternally
            set(value) = definedExternally
        var minimise: String?
            get() = definedExternally
            set(value) = definedExternally
        var popout: String?
            get() = definedExternally
            set(value) = definedExternally
    }

    interface ItemConfig {
        var type: String
        var content: Array<ItemConfig>?
            get() = definedExternally
            set(value) = definedExternally
        var width: Number?
            get() = definedExternally
            set(value) = definedExternally
        var height: Number?
            get() = definedExternally
            set(value) = definedExternally
        var id: dynamic /* String? | Array<String>? */
            get() = definedExternally
            set(value) = definedExternally
        var isClosable: Boolean?
            get() = definedExternally
            set(value) = definedExternally
        var title: String?
            get() = definedExternally
            set(value) = definedExternally
    }

    interface ComponentConfig : ItemConfig {
        var componentName: String
        var componentState: Any?
            get() = definedExternally
            set(value) = definedExternally
    }

    interface ReactComponentConfig : ItemConfig {
        var component: String
        var props: Any?
            get() = definedExternally
            set(value) = definedExternally
    }

    interface Config {
        var settings: Settings?
            get() = definedExternally
            set(value) = definedExternally
        var dimensions: Dimensions?
            get() = definedExternally
            set(value) = definedExternally
        var labels: Labels?
            get() = definedExternally
            set(value) = definedExternally
        var content: Array<dynamic /* ItemConfig | ComponentConfig | ReactComponentConfig */>?
            get() = definedExternally
            set(value) = definedExternally
    }

    interface ContentItem : EventEmitter {
        var config: dynamic /* ItemConfig | ComponentConfig | ReactComponentConfig */
            get() = definedExternally
            set(value) = definedExternally
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
