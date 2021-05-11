package world.phantasmal.observable.cell.list

import world.phantasmal.core.unsafe.unsafeAssertNotNull
import world.phantasmal.observable.cell.Cell

/**
 * ListCell of which the value depends on 0 or more other cells.
 */
class DependentListCell<E>(
    vararg dependencies: Cell<*>,
    private val computeElements: () -> List<E>,
) : AbstractDependentListCell<E>(*dependencies) {
    private var _elements: List<E>? = null

    override val elements: List<E> get() = _elements.unsafeAssertNotNull()

    override fun computeElements() {
        _elements = computeElements.invoke()
    }
}
