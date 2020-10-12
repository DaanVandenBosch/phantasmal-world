package world.phantasmal.webui.widgets

import kotlinx.browser.document
import kotlinx.dom.appendText
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.Node
import world.phantasmal.core.disposable.DisposableContainer
import world.phantasmal.core.disposable.disposable
import world.phantasmal.observable.Observable
import world.phantasmal.observable.Observer
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.observable.value.or
import kotlin.reflect.KClass

abstract class Widget(
    style: () -> String = NO_STYLE,
    val hidden: Val<Boolean> = falseVal(),
) : DisposableContainer() {
    private val _ancestorHidden = mutableVal(false)
    private val _children = mutableListOf<Widget>()
    private var initResizeObserverRequested = false
    private var resizeObserverInitialized = false

    private val elementDelegate = lazy {
        // Add CSS declarations to stylesheet if this is the first time we're instantiating this
        // widget.
        if (style !== NO_STYLE && STYLES_ADDED.add(this::class)) {
            STYLE_EL.appendText(style())
        }

        val el = document.createDocumentFragment().createElement()

        observe(hidden) { hidden ->
            el.hidden = hidden
            children.forEach { setAncestorHidden(it, hidden || ancestorHidden.value) }
        }

        if (initResizeObserverRequested) {
            initResizeObserver(el)
        }

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

    protected abstract fun Node.createElement(): HTMLElement

    override fun internalDispose() {
        if (elementDelegate.isInitialized()) {
            element.remove()
        }

        _children.clear()
        super.internalDispose()
    }

    protected fun <V1> observe(o1: Observable<V1>, operation: (V1) -> Unit) {
        if (o1 is Val<V1>) {
            addDisposable(o1.observe(callNow = true) { operation(it.value) })
        } else {
            addDisposable(o1.observe { operation(it.value) })
        }
    }

    protected fun <V1, V2> observe(
        v1: Val<V1>,
        v2: Val<V2>,
        operation: (V1, V2) -> Unit,
    ) {
        val observer: Observer<*> = {
            operation(v1.value, v2.value)
        }
        addDisposable(v1.observe(observer))
        addDisposable(v2.observe(observer))
        operation(v1.value, v2.value)
    }

    protected fun <V1, V2, V3> observe(
        v1: Val<V1>,
        v2: Val<V2>,
        v3: Val<V3>,
        operation: (V1, V2, V3) -> Unit,
    ) {
        val observer: Observer<*> = {
            operation(v1.value, v2.value, v3.value)
        }
        addDisposable(v1.observe(observer))
        addDisposable(v2.observe(observer))
        addDisposable(v3.observe(observer))
        operation(v1.value, v2.value, v3.value)
    }

    protected fun <V1, V2, V3, V4> observe(
        v1: Val<V1>,
        v2: Val<V2>,
        v3: Val<V3>,
        v4: Val<V4>,
        operation: (V1, V2, V3, V4) -> Unit,
    ) {
        val observer: Observer<*> = {
            operation(v1.value, v2.value, v3.value, v4.value)
        }
        addDisposable(v1.observe(observer))
        addDisposable(v2.observe(observer))
        addDisposable(v3.observe(observer))
        addDisposable(v4.observe(observer))
        operation(v1.value, v2.value, v3.value, v4.value)
    }

    protected fun <V1, V2, V3, V4, V5> observe(
        v1: Val<V1>,
        v2: Val<V2>,
        v3: Val<V3>,
        v4: Val<V4>,
        v5: Val<V5>,
        operation: (V1, V2, V3, V4, V5) -> Unit,
    ) {
        val observer: Observer<*> = {
            operation(v1.value, v2.value, v3.value, v4.value, v5.value)
        }
        addDisposable(v1.observe(observer))
        addDisposable(v2.observe(observer))
        addDisposable(v3.observe(observer))
        addDisposable(v4.observe(observer))
        addDisposable(v5.observe(observer))
        operation(v1.value, v2.value, v3.value, v4.value, v5.value)
    }

    /**
     * Adds a child widget to [children] and makes sure it is disposed when this widget is disposed.
     */
    protected fun <T : Widget> Node.addChild(child: T): T {
        _children.add(child)
        setAncestorHidden(child, selfOrAncestorHidden.value)
        appendChild(child.element)
        return addDisposable(child)
    }

    /**
     * Removes a child widget from [children] and disposes it.
     */
    protected fun removeChild(child: Widget) {
        _children.remove(child)
        removeDisposable(child)
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
        private val STYLES_ADDED: MutableSet<KClass<out Widget>> = mutableSetOf()

        protected val NO_STYLE = { "" }

        protected fun setAncestorHidden(widget: Widget, hidden: Boolean) {
            widget._ancestorHidden.value = hidden

            if (widget.hidden.value) return

            widget.children.forEach {
                setAncestorHidden(it, widget.selfOrAncestorHidden.value)
            }
        }
    }
}
