package world.phantasmal.observable.cell

import world.phantasmal.core.unsafe.unsafeCast
import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.Dependent

abstract class AbstractDependentCell<T> : AbstractCell<T>(), Dependent {

    private var _value: T? = null
    final override val value: T
        get() {
            computeValueAndEvent()
            // We cast instead of asserting _value is non-null because T might actually be a
            // nullable type.
            return unsafeCast(_value)
        }

    final override var changeEvent: ChangeEvent<T>? = null
        get() {
            computeValueAndEvent()
            return field
        }
        private set

    protected abstract fun computeValueAndEvent()

    protected fun setValueAndEvent(value: T, changeEvent: ChangeEvent<T>?) {
        _value = value
        this.changeEvent = changeEvent
    }
}
