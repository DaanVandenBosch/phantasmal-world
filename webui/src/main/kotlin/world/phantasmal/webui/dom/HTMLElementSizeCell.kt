package world.phantasmal.webui.dom

import org.w3c.dom.HTMLElement
import world.phantasmal.core.unsafe.unsafeAssertNotNull
import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.Dependent
import world.phantasmal.observable.cell.AbstractCell

data class Size(val width: Double, val height: Double)

class HTMLElementSizeCell(element: HTMLElement? = null) : AbstractCell<Size>() {
    private var resizeObserver: dynamic = null

    private var _value: Size? = null

    var element: HTMLElement? = element
        set(element) {
            if (resizeObserver != null) {
                if (field != null) {
                    resizeObserver.unobserve(field)
                }

                if (element != null) {
                    resizeObserver.observe(element)
                }
            }

            field = element
        }

    override val value: Size
        get() {
            if (dependents.isEmpty()) {
                _value = getSize()
            }

            return unsafeAssertNotNull(_value)
        }

    override fun addDependent(dependent: Dependent) {
        if (dependents.isEmpty()) {
            if (resizeObserver == null) {
                @Suppress("UNUSED_VARIABLE")
                val resize = ::resizeCallback
                resizeObserver = js("new ResizeObserver(resize);")
            }

            if (element != null) {
                resizeObserver.observe(element)
            }

            _value = getSize()
        }

        super.addDependent(dependent)
    }

    override fun removeDependent(dependent: Dependent) {
        super.removeDependent(dependent)

        if (dependents.isEmpty()) {
            resizeObserver.disconnect()
        }
    }

    override fun emitDependencyChanged() {
        error("HTMLElementSizeCell emits dependencyChanged immediately.")
    }

    private fun getSize(): Size =
        element
            ?.let { Size(it.offsetWidth.toDouble(), it.offsetHeight.toDouble()) }
            ?: Size(0.0, 0.0)

    private fun resizeCallback(entries: Array<dynamic>) {
        val entry = entries.first()
        val newValue = Size(
            entry.contentRect.width.unsafeCast<Double>(),
            entry.contentRect.height.unsafeCast<Double>(),
        )

        if (newValue != _value) {
            emitMightChange()
            _value = newValue
            emitDependencyChanged(ChangeEvent(newValue))
        }
    }
}
