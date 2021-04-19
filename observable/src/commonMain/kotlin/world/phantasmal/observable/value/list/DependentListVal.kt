package world.phantasmal.observable.value.list

import world.phantasmal.core.unsafe.unsafeAssertNotNull
import world.phantasmal.observable.value.Val

/**
 * ListVal of which the value depends on 0 or more other vals.
 */
class DependentListVal<E>(
    vararg dependencies: Val<*>,
    private val computeElements: () -> List<E>,
) : AbstractDependentListVal<E>(*dependencies) {
    private var _elements: List<E>? = null

    override val elements: List<E> get() = _elements.unsafeAssertNotNull()

    override fun computeElements() {
        _elements = computeElements.invoke()
    }
}
