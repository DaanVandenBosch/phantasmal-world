package world.phantasmal.webui.dom

import org.w3c.dom.HTMLElement
import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.disposable
import world.phantasmal.core.unsafe.unsafeAssertNotNull
import world.phantasmal.observable.Observer
import world.phantasmal.observable.value.AbstractVal

data class Size(val width: Double, val height: Double)

class HTMLElementSizeVal(element: HTMLElement? = null) : AbstractVal<Size>() {
    private var resizeObserver: dynamic = null

    /**
     * Set to true right before actual observers are added.
     */
    private var hasObservers = false

    private var _value: Size? = null

    var element: HTMLElement? = null
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

    init {
        // Ensure we call the setter with element.
        this.element = element
    }

    override val value: Size
        get() {
            if (!hasObservers) {
                _value = getSize()
            }

            return _value.unsafeAssertNotNull()
        }

    override fun observe(callNow: Boolean, observer: Observer<Size>): Disposable {
        if (!hasObservers) {
            hasObservers = true

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

        val superDisposable = super.observe(callNow, observer)

        return disposable {
            superDisposable.dispose()

            if (observers.isEmpty()) {
                hasObservers = false
                resizeObserver.disconnect()
            }
        }
    }

    private fun getSize(): Size =
        element
            ?.let { Size(it.offsetWidth.toDouble(), it.offsetHeight.toDouble()) }
            ?: Size(0.0, 0.0)

    private fun resizeCallback(entries: Array<dynamic>) {
        entries.forEach { entry ->
            _value = Size(
                entry.contentRect.width.unsafeCast<Double>(),
                entry.contentRect.height.unsafeCast<Double>()
            )
            emit()
        }
    }
}
