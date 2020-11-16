package world.phantasmal.webui.widgets

import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import org.w3c.dom.*
import org.w3c.dom.pointerevents.PointerEvent
import world.phantasmal.observable.Observable
import world.phantasmal.observable.value.*
import world.phantasmal.observable.value.list.ListVal
import world.phantasmal.observable.value.list.ListValChangeEvent
import world.phantasmal.webui.DisposableContainer
import world.phantasmal.webui.dom.HTMLElementSizeVal
import world.phantasmal.webui.dom.Size
import world.phantasmal.webui.dom.disposablePointerDrag
import world.phantasmal.webui.dom.documentFragment

abstract class Widget(
    protected val scope: CoroutineScope,
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

    override fun internalDispose() {
        if (elementDelegate.isInitialized()) {
            element.remove()
        }

        _children.clear()
        super.internalDispose()
    }

    protected fun Node.text(observable: Observable<String>) {
        observe(observable) { textContent = it }
    }

    protected fun HTMLElement.hidden(observable: Observable<Boolean>) {
        observe(observable) { hidden = it }
    }

    /**
     * Appends a widget's element to the receiving node.
     */
    protected fun <T : Widget> Node.addWidget(widget: T): T {
        addDisposable(widget)
        appendChild(widget.element)
        return widget
    }

    /**
     * Adds a child widget to [children] and appends its element to the receiving node.
     */
    protected fun <T : Widget> Node.addChild(child: T): T {
        addDisposable(child)
        _children.add(child)
        setAncestorVisible(child, selfOrAncestorVisible.value)
        appendChild(child.element)
        return child
    }

    protected fun <T> Element.bindChildrenTo(
        list: Val<List<T>>,
        createChild: Node.(T, Int) -> Node,
    ) {
        if (list is ListVal) {
            bindChildrenTo(list, createChild)
        } else {
            observe(list) { items ->
                innerHTML = ""

                val frag = document.createDocumentFragment()

                items.forEachIndexed { i, item ->
                    frag.createChild(item, i)
                }

                appendChild(frag)
            }
        }
    }

    protected fun <T> Element.bindChildrenTo(
        list: ListVal<T>,
        createChild: Node.(T, Int) -> Node,
    ) {
        fun spliceChildren(index: Int, removedCount: Int, inserted: List<T>) {
            for (i in 1..removedCount) {
                removeChild(childNodes[index].unsafeCast<Node>())
            }

            val frag = document.createDocumentFragment()

            inserted.forEachIndexed { i, value ->
                frag.createChild(value, index + i)
            }

            if (index >= childNodes.length) {
                appendChild(frag)
            } else {
                insertBefore(frag, childNodes[index])
            }
        }

        addDisposable(
            list.observeList { change: ListValChangeEvent<T> ->
                when (change) {
                    is ListValChangeEvent.Change -> {
                        spliceChildren(change.index, change.removed.size, change.inserted)
                    }
                    is ListValChangeEvent.ElementChange -> {
                        // TODO: Update children.
                    }
                }
            }
        )

        spliceChildren(0, 0, list.value)
    }

    fun Element.onDrag(
        onPointerDown: (e: PointerEvent) -> Boolean,
        onPointerMove: (movedX: Int, movedY: Int, e: PointerEvent) -> Boolean,
        onPointerUp: (e: PointerEvent) -> Unit = {},
    ) {
        addDisposable(disposablePointerDrag(onPointerDown, onPointerMove, onPointerUp))
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
