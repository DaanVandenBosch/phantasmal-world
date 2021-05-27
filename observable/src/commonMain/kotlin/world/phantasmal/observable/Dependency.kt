package world.phantasmal.observable

interface Dependency {
    /**
     * This method is not meant to be called from typical application code. Usually you'll want to
     * use [Observable.observe].
     */
    fun addDependent(dependent: Dependent)

    /**
     * This method is not meant to be called from typical application code.
     */
    fun removeDependent(dependent: Dependent)
}
