package world.phantasmal.cell

/**
 * Cell of which the value depends on 0 or more dependencies.
 */
class DependentCell<T>(
    private vararg val dependencies: Cell<*>,
    private val compute: () -> T,
) : AbstractDependentCell<T, ChangeEvent<T>>() {

    private var valid = false

    override fun computeValueAndEvent() {
        // Recompute value every time when we have no dependents. At that point we're not yet a
        // dependent of our own dependencies, and thus we won't automatically recompute our value
        // when they change.
        if (!valid) {
            val newValue = compute()
            _value = newValue
            changeEvent = ChangeEvent(newValue)
            valid = dependents.isNotEmpty()
        }
    }

    override fun addDependent(dependent: Dependent) {
        if (dependents.isEmpty()) {
            // Start actually depending on our dependencies when we get our first dependent.
            for (dependency in dependencies) {
                dependency.addDependent(this)
            }
        }

        super.addDependent(dependent)
    }

    override fun removeDependent(dependent: Dependent) {
        super.removeDependent(dependent)

        if (dependents.isEmpty()) {
            // As long as we don't have any dependents we're permanently "invalid", i.e. we always
            // recompute our value.
            valid = false

            // Stop actually depending on our dependencies when we no longer have any dependents.
            for (dependency in dependencies) {
                dependency.removeDependent(this)
            }
        }
    }

    override fun dependencyInvalidated(dependency: Dependency<*>) {
        valid = false
        emitDependencyInvalidated()
    }
}
