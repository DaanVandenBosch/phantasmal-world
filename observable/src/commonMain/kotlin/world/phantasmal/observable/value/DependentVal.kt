package world.phantasmal.observable.value

/**
 * Val of which the value depends on 0 or more other vals.
 */
class DependentVal<T>(
    dependencies: Iterable<Val<*>>,
    private val compute: () -> T,
) : AbstractDependentVal<T>(dependencies) {
    override fun computeValue(): T = compute()
}
