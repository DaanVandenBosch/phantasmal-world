package world.phantasmal.observable.cell

/**
 * Cell of which the value depends on 0 or more other cells.
 */
class DependentCell<T>(
    vararg dependencies: Cell<*>,
    private val compute: () -> T,
) : AbstractDependentCell<T>(*dependencies) {
    override fun computeValue(): T = compute()
}
