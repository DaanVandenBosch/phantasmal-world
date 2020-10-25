package world.phantasmal.webui.widgets

import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.dom.appendText
import kotlinx.dom.clear
import org.w3c.dom.*
import world.phantasmal.core.disposable.disposable
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal
import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.list.ListValChangeEvent
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.observable.value.or
import world.phantasmal.webui.DisposableContainer

abstract class Widget(
    protected val scope: CoroutineScope,
    private val styles: List<() -> String> = emptyList(),
    /**
     * By default determines the hidden attribute of its [element].
     */
    val hidden: Val<Boolean> = falseVal(),
    /**
     * By default determines the disabled attribute of its [element] and whether or not the
     * `pw-disabled` class is added.
     */
    val disabled: Val<Boolean> = falseVal(),
) : DisposableContainer() {
    private val _ancestorHidden = mutableVal(false)
    private val _children = mutableListOf<Widget>()
    private var initResizeObserverRequested = false
    private var resizeObserverInitialized = false

    private val elementDelegate = lazy {
        // Add CSS declarations to stylesheet if this is the first time we're encountering them.
        styles.forEach { style ->
            if (STYLES_ADDED.add(style)) {
                STYLE_EL.appendText(style())
            }
        }

        val el = document.createDocumentFragment().createElement()

        observe(hidden) { hidden ->
            el.hidden = hidden
            children.forEach { setAncestorHidden(it, hidden || ancestorHidden.value) }
        }

        observe(disabled) { disabled ->
            if (disabled) {
                el.setAttribute("disabled", "")
                el.classList.add("pw-disabled")
            } else {
                el.removeAttribute("disabled")
                el.classList.remove("pw-disabled")
            }
        }

        if (initResizeObserverRequested) {
            initResizeObserver(el)
        }

        interceptElement(el)
        el
    }

    /**
     * This widget's outermost DOM element.
     */
    val element: HTMLElement by elementDelegate

    /**
     * True if any of this widget's ancestors are [hidden], false otherwise.
     */
    val ancestorHidden: Val<Boolean> = _ancestorHidden

    /**
     * True if this widget or any of its ancestors are [hidden], false otherwise.
     */
    val selfOrAncestorHidden: Val<Boolean> = hidden or ancestorHidden

    val children: List<Widget> = _children

    /**
     * Called to initialize [element] when it is first accessed.
     */
    protected abstract fun Node.createElement(): HTMLElement

    /**
     * Called right after [createElement] and the default initialization for [element] is done.
     */
    protected open fun interceptElement(element: HTMLElement) {}

    override fun internalDispose() {
        if (elementDelegate.isInitialized()) {
            element.remove()
        }

        _children.clear()
        super.internalDispose()
    }

    /**
     * Adds a child widget to [children] and appends its element to the receiving node.
     */
    protected fun <T : Widget> Node.addChild(child: T): T {
        addDisposable(child)
        _children.add(child)
        setAncestorHidden(child, selfOrAncestorHidden.value)
        appendChild(child.element)
        return child
    }

    fun <T> Node.bindChildrenTo(
        list: ListVal<T>,
        createChild: (T, Int) -> Node,
    ) {
        fun spliceChildren(index: Int, removedCount: Int, inserted: List<T>) {
            for (i in 1..removedCount) {
                removeChild(childNodes[index].unsafeCast<Node>())
            }

            val frag = document.createDocumentFragment()

            inserted.forEachIndexed { i, value ->
                val child = createChild(value, index + i)

                frag.append(child)
            }

            if (index >= childNodes.length) {
                appendChild(frag)
            } else {
                insertBefore(frag, childNodes[index])
            }
        }

        val observer = list.observeList { change: ListValChangeEvent<T> ->
            when (change) {
                is ListValChangeEvent.Change -> {
                    spliceChildren(change.index, change.removed.size, change.inserted)
                }
                is ListValChangeEvent.ElementChange -> {
                    // TODO: Update children.
                }
            }
        }

        spliceChildren(0, 0, list.value)

        addDisposable(
            disposable {
                observer.dispose()
                clear()
            }
        )
    }

    /**
     * Called whenever [element] is resized.
     * Must be initialized with [observeResize].
     */
    protected open fun resized(width: Double, height: Double) {}

    protected fun observeResize() {
        if (elementDelegate.isInitialized()) {
            initResizeObserver(element)
        } else {
            initResizeObserverRequested = true
        }
    }

    private fun initResizeObserver(element: Element) {
        if (resizeObserverInitialized) return

        resizeObserverInitialized = true
        @Suppress("UNUSED_VARIABLE")
        val resize = ::resizeCallback
        val observer = js("new ResizeObserver(resize);")
        observer.observe(element)
        addDisposable(disposable { observer.disconnect().unsafeCast<Unit>() })
    }

    private fun resizeCallback(entries: Array<dynamic>) {
        entries.forEach { entry ->
            resized(
                entry.contentRect.width.unsafeCast<Double>(),
                entry.contentRect.height.unsafeCast<Double>()
            )
        }
    }

    companion object {
        private val STYLE_EL by lazy {
            val el = document.createElement("style") as HTMLStyleElement
            el.id = "pw-widget-styles"
            document.head!!.append(el)
            el
        }
        private val STYLES_ADDED: MutableSet<() -> String> = mutableSetOf()

        protected fun setAncestorHidden(widget: Widget, hidden: Boolean) {
            widget._ancestorHidden.value = hidden

            if (widget.hidden.value) return

            widget.children.forEach {
                setAncestorHidden(it, widget.selfOrAncestorHidden.value)
            }
        }
    }
}
