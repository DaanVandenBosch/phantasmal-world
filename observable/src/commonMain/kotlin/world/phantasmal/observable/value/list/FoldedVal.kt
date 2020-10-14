package world.phantasmal.observable.value.list

import world.phantasmal.core.disposable.DisposableScope
import world.phantasmal.core.disposable.Scope
import world.phantasmal.core.disposable.disposable
import world.phantasmal.core.fastCast
import world.phantasmal.observable.value.AbstractVal
import world.phantasmal.observable.value.ValObserver

class FoldedVal<T, R>(
    private val dependency: ListVal<T>,
    private val initial: R,
    private val operation: (R, T) -> R,
) : AbstractVal<R>() {
    private var dependencyDisposable = DisposableScope()
    private var internalValue: R? = null

    override val value: R
        get() {
            return if (dependencyDisposable.isEmpty()) {
                computeValue()
            } else {
                internalValue.fastCast()
            }
        }

    override fun observe(scope: Scope, callNow: Boolean, observer: ValObserver<R>) {
        super.observe(scope, callNow, observer)

        if (dependencyDisposable.isEmpty()) {
            internalValue = computeValue()

            dependency.observe(dependencyDisposable) {
                val oldValue = internalValue
                internalValue = computeValue()
                emit(oldValue.fastCast())
            }
        }

        scope.disposable {
            if (observers.isEmpty()) {
                dependencyDisposable.disposeAll()
            }
        }
    }

    private fun computeValue(): R = dependency.value.fold(initial, operation)
}
