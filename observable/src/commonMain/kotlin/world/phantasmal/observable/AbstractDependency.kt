package world.phantasmal.observable

abstract class AbstractDependency : Dependency {
    protected val dependents: MutableList<Dependent> = mutableListOf()

    override fun addDependent(dependent: Dependent) {
        dependents.add(dependent)
    }

    override fun removeDependent(dependent: Dependent) {
        dependents.remove(dependent)
    }
}
