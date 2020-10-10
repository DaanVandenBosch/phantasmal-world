package world.phantasmal.observable.value

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.disposable
import world.phantasmal.core.fastCast

class DependentVal<T>(
    private val dependencies: Iterable<Val<*>>,
    private val operation: () -> T,
) : AbstractVal<T>() {
    private var dependencyDisposables: MutableList<Disposable> = mutableListOf()
    private var internalValue: T? = null

    override val value: T
        get() {
            return if (dependencyDisposables.isEmpty()) {
                operation()
            } else {
                internalValue.fastCast()
            }
        }

    override fun observe(callNow: Boolean, observer: ValObserver<T>): Disposable {
        if (dependencyDisposables.isEmpty()) {
            internalValue = operation()

            dependencyDisposables.addAll(dependencies.map { dependency ->
                dependency.observe {
                    val oldValue = internalValue
                    internalValue = operation()
                    emit(oldValue.fastCast())
                }
            })
        }

        val superDisposable = super.observe(callNow, observer)

        return disposable {
            superDisposable.dispose()

            if (observers.isEmpty()) {
                dependencyDisposables.forEach { it.dispose() }
                dependencyDisposables.clear()
            }
        }
    }
}
