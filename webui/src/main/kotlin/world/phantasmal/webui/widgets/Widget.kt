package world.phantasmal.webui.widgets

import kotlinx.browser.document
import kotlinx.dom.appendText
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.Node
import world.phantasmal.core.disposable.Scope
import world.phantasmal.core.disposable.TrackedDisposable
import world.phantasmal.core.disposable.disposable
import world.phantasmal.observable.Observable
import world.phantasmal.observable.Observer
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.observable.value.or
import kotlin.reflect.KClass

abstract class Widget(
    protected val scope: Scope,
    style: () -> String = NO_STYLE,
    /**
     * By default determines the hidden attribute of its [element].
     */
    val hidden: Val<Boolean> = falseVal(),
    /**
     * By default determines the disabled attribute of its [element] and whether or not the
     * `pw-disabled` class is added.
     */
    val disabled: Val<Boolean> = falseVal(),
) : TrackedDisposable(scope.scope()) {
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

        hidden.observe { hidden ->
            el.hidden = hidden
            children.forEach { setAncestorHidden(it, hidden || ancestorHidden.value) }
        }

        disabled.observe { disabled ->
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
    }

    protected fun <V1> Observable<V1>.observe(operation: (V1) -> Unit) {
        if (this is Val<V1>) {
            this.observe(scope, callNow = true) { operation(it.value) }
        } else {
            this.observe(scope) { operation(it.value) }
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
        v1.observe(scope, observer)
        v2.observe(scope, observer)
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
        v1.observe(scope, observer)
        v2.observe(scope, observer)
        v3.observe(scope, observer)
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
        v1.observe(scope, observer)
        v2.observe(scope, observer)
        v3.observe(scope, observer)
        v4.observe(scope, observer)
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
        v1.observe(scope, observer)
        v2.observe(scope, observer)
        v3.observe(scope, observer)
        v4.observe(scope, observer)
        v5.observe(scope, observer)
        operation(v1.value, v2.value, v3.value, v4.value, v5.value)
    }

    /**
     * Adds a child widget to [children].
     */
    protected fun <T : Widget> Node.addChild(child: T): T {
        _children.add(child)
        setAncestorHidden(child, selfOrAncestorHidden.value)
        appendChild(child.element)
        return child
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
        scope.disposable { observer.disconnect().unsafeCast<Unit>() }
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
