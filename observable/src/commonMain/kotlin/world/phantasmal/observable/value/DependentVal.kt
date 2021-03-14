package world.phantasmal.observable.value

class DependentVal<T>(
    dependencies: Iterable<Val<*>>,
    private val compute: () -> T,
) : AbstractDependentVal<T>(dependencies) {
    override fun computeValue(): T = compute()
}
