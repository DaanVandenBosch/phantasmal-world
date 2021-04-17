package world.phantasmal.webui.widgets

import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.Node
import org.w3c.dom.pointerevents.PointerEvent
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.DisposableSupervisedScope
import world.phantasmal.core.disposable.disposable
import world.phantasmal.observable.Observable
import world.phantasmal.observable.value.*
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.dom.*

abstract class Widget(
    /**
     * By default determines the hidden attribute of its [element].
     */
    val visible: Val<Boolean> = trueVal(),
    /**
     * By default determines the disabled attribute of its [element] and whether or not the
     * "pw-disabled" class is added.
     */
    val enabled: Val<Boolean> = trueVal(),
    val tooltip: Val<String?> = nullVal(),
) : DisposableContainer() {
    private val _ancestorVisible = mutableVal(true)
    private val _children = mutableListOf<Widget>()
    private val _size = HTMLElementSizeVal()

    private val elementDelegate = lazy {
        val el = documentFragment().createElement()

        observe(visible) { visible ->
            el.hidden = !visible
            children.forEach { setAncestorVisible(it, visible && ancestorVisible.value) }
        }

        observe(enabled) { enabled ->
            if (enabled) {
                el.removeAttribute("disabled")
                el.classList.remove("pw-disabled")
            } else {
                el.setAttribute("disabled", "")
                el.classList.add("pw-disabled")
            }
        }

        observe(tooltip) { tooltip ->
            if (tooltip == null) {
                el.removeAttribute("title")
            } else {
                el.title = tooltip
            }
        }

        _size.element = el

        interceptElement(el)
        el
    }

    protected val scope by lazy {
        addDisposable(DisposableSupervisedScope(this::class, Dispatchers.Main))
    }

    /**
     * This widget's outermost DOM element.
     */
    val element: HTMLElement by elementDelegate

    /**
     * True if this widget's ancestors are [visible], false otherwise.
     */
    val ancestorVisible: Val<Boolean> = _ancestorVisible

    /**
     * True if this widget and all of its ancestors are [visible], false otherwise.
     */
    val selfOrAncestorVisible: Val<Boolean> = visible and ancestorVisible

    val size: Val<Size> = _size

    val children: List<Widget> = _children

    open fun focus() {
        element.focus()
    }

    /**
     * Called to initialize [element] when it is first accessed.
     */
    protected abstract fun Node.createElement(): HTMLElement

    /**
     * Called right after [createElement] and the default initialization for [element] is done.
     */
    protected open fun interceptElement(element: HTMLElement) {}

    override fun dispose() {
        if (elementDelegate.isInitialized()) {
            element.remove()
        }

        _children.clear()
        super.dispose()
    }

    protected fun Node.text(observable: Observable<String>) {
        observe(observable) { textContent = it }
    }

    protected fun HTMLElement.hidden(observable: Observable<Boolean>) {
        observe(observable) { hidden = it }
    }

    protected fun HTMLElement.toggleClass(className: String, observable: Observable<Boolean>) {
        observe(observable) {
            if (it) classList.add(className)
            else classList.remove(className)
        }
    }

    /**
     * Appends a widget's element to the receiving node.
     */
    protected fun <T : Widget> Node.addWidget(widget: T, addToDisposer: Boolean = true): T {
        if (addToDisposer) {
            addDisposable(widget)
        }

        appendChild(widget.element)
        return widget
    }

    /**
     * Adds a child widget to [children] and appends its element to the receiving node.
     */
    protected fun <T : Widget> Node.addChild(child: T, addToDisposer: Boolean = true): T {
        if (addToDisposer) {
            addDisposable(child)
        }

        _children.add(child)
        setAncestorVisible(child, selfOrAncestorVisible.value)
        appendChild(child.element)
        return child
    }

    /**
     * Removes a child widget from [children] and disposes it.
     */
    protected fun removeChild(child: Widget, dispose: Boolean = true) {
        removeDisposable(child, dispose)
        _children.remove(child)
    }

    protected fun <T> Element.bindChildrenTo(
        list: Val<List<T>>,
        createChild: Node.(T, index: Int) -> Node,
    ) {
        addDisposable(bindChildrenTo(this, list, createChild))
    }

    protected fun <T> Element.bindDisposableChildrenTo(
        list: Val<List<T>>,
        createChild: Node.(T, index: Int) -> Pair<Node, Disposable>,
    ) {
        addDisposable(bindDisposableChildrenTo(this, list, createChild))
    }

    /**
     * Creates a widget for every element in [list] and adds it as a child.
     */
    protected fun <T> Element.bindChildWidgetsTo(
        list: Val<List<T>>,
        createChild: (T, index: Int) -> Widget,
    ) {
        val create: Node.(T, Int) -> Pair<Node, Disposable> = { value: T, index: Int ->
            val widget = createChild(value, index)
            addChild(widget, addToDisposer = false)

            Pair<Node, Disposable>(
                widget.element,
                disposable {
                    removeChild(widget, dispose = false)
                    widget.dispose()
                }
            )
        }

        addDisposable(bindDisposableChildrenTo(this, list, create))
    }

    fun Element.onDrag(
        onPointerDown: (e: PointerEvent) -> Boolean,
        onPointerMove: (movedX: Int, movedY: Int, e: PointerEvent) -> Boolean,
        onPointerUp: (e: PointerEvent) -> Unit = {},
    ) {
        addDisposable(disposablePointerDrag(onPointerDown, onPointerMove, onPointerUp))
    }

    protected fun launch(block: suspend CoroutineScope.() -> Unit) {
        scope.launch(block = block)
    }

    companion object {
        private val STYLE_EL by lazy {
            val el = document.createElement("style") as HTMLStyleElement
            el.id = "pw-widget-styles"
            document.head!!.append(el)
            el
        }

        init {
            js("require('@fortawesome/fontawesome-free/js/fontawesome');")
            js("require('@fortawesome/fontawesome-free/js/solid');")
            js("require('@fortawesome/fontawesome-free/js/regular');")
            js("require('@fortawesome/fontawesome-free/js/brands');")
        }

        protected fun style(style: String) {
            STYLE_EL.append(style)
        }

        protected fun setAncestorVisible(widget: Widget, visible: Boolean) {
            widget._ancestorVisible.value = visible

            if (!widget.visible.value) return

            widget.children.forEach {
                setAncestorVisible(it, widget.selfOrAncestorVisible.value)
            }
        }
    }
}
