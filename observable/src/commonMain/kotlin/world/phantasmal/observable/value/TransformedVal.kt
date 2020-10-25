package world.phantasmal.observable.value

class TransformedVal<T>(
    dependencies: Iterable<Val<*>>,
    private val compute: () -> T,
) : DependentVal<T>(dependencies) {
    override fun computeValue(): T = compute()
}
