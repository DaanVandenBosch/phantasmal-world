package world.phantasmal.webui.dom

import org.w3c.dom.HTMLElement
import world.phantasmal.core.ResizeObserver
import world.phantasmal.core.ResizeObserverEntry
import world.phantasmal.core.unsafe.unsafeAssertNotNull
import world.phantasmal.cell.ChangeEvent
import world.phantasmal.cell.Dependent
import world.phantasmal.cell.AbstractCell

data class Size(val width: Double, val height: Double)

class HTMLElementSizeCell : AbstractCell<Size>() {
    private var resizeObserver: ResizeObserver? = null

    private var element: HTMLElement? = null

    private var _value: Size? = null
    override val value: Size
        get() {
            computeValueAndEvent()
            return unsafeAssertNotNull(_value)
        }

    override var changeEvent: ChangeEvent<Size>? = null
        get() {
            computeValueAndEvent()
            return field
        }
        private set

    private fun computeValueAndEvent() {
        if (dependents.isEmpty()) {
            setValueAndEvent()
        }
    }

    fun setElement(element: HTMLElement) {
        check(this.element == null) { "setElement should be called at most once." }

        this.element = element
        resizeObserver?.observe(element)
    }

    override fun addDependent(dependent: Dependent) {
        if (dependents.isEmpty()) {
            if (resizeObserver == null) {
                resizeObserver = ResizeObserver(::resizeCallback)
            }

            element?.let(unsafeAssertNotNull(resizeObserver)::observe)

            setValueAndEvent()
        }

        super.addDependent(dependent)
    }

    override fun removeDependent(dependent: Dependent) {
        super.removeDependent(dependent)

        if (dependents.isEmpty()) {
            resizeObserver?.disconnect()
        }
    }

    private fun setValueAndEvent() {
        val size = element
            ?.let { Size(it.offsetWidth.toDouble(), it.offsetHeight.toDouble()) }
            ?: ZERO_SIZE

        _value = size
        changeEvent = ChangeEvent(size)
    }

    private fun resizeCallback(entries: Array<ResizeObserverEntry>) {
        val oldValue = _value
        val entry = entries.first()
        val width = entry.contentRect.width
        val height = entry.contentRect.height

        if (oldValue == null || width != oldValue.width || height != oldValue.height) {
            applyChange {
                val newValue = Size(width, height)
                _value = newValue
                changeEvent = ChangeEvent(newValue)
            }
        }
    }

    companion object {
        private val ZERO_SIZE = Size(0.0, 0.0)
    }
}
