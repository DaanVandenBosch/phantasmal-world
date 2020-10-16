package world.phantasmal.observable.value

import world.phantasmal.core.disposable.DisposableScope
import world.phantasmal.core.disposable.Scope
import world.phantasmal.core.disposable.disposable
import world.phantasmal.core.fastCast
import kotlin.coroutines.EmptyCoroutineContext

class DependentVal<T>(
    private val dependencies: Iterable<Val<*>>,
    private val operation: () -> T,
) : AbstractVal<T>() {
    private var dependencyScope = DisposableScope(EmptyCoroutineContext)
    private var internalValue: T? = null

    override val value: T
        get() {
            return if (dependencyScope.isEmpty()) {
                operation()
            } else {
                internalValue.fastCast()
            }
        }

    override fun observe(scope: Scope, callNow: Boolean, observer: ValObserver<T>) {
        if (dependencyScope.isEmpty()) {
            internalValue = operation()

            dependencies.forEach { dependency ->
                dependency.observe(dependencyScope) {
                    val oldValue = internalValue
                    internalValue = operation()
                    emit(oldValue.fastCast())
                }
            }
        }

        super.observe(scope, callNow, observer)

        scope.disposable {
            if (observers.isEmpty()) {
                dependencyScope.disposeAll()
            }
        }
    }
}
