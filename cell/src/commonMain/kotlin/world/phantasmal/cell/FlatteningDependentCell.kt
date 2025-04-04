package world.phantasmal.cell

/**
 * Similar to [DependentCell], except that this cell's [compute] returns a cell.
 */
internal class FlatteningDependentCell<T>(
    vararg dependencies: Cell<*>,
    compute: () -> Cell<T>,
) : AbstractFlatteningDependentCell<T, Cell<T>, ChangeEvent<T>>(dependencies, compute) {
    override fun transformNewValue(value: T): T = value

    override fun createEvent(oldValue: T?, newValue: T): ChangeEvent<T> =
        ChangeEvent(newValue)
}
