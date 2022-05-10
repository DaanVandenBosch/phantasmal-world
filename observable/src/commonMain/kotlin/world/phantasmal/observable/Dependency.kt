package world.phantasmal.observable

interface Dependency<out T> {
    // TODO: Docs.
    val changeEvent: ChangeEvent<T>?

    /**
     * This method is not meant to be called from typical application code. Usually you'll want to
     * use [Observable.observeChange].
     */
    fun addDependent(dependent: Dependent)

    /**
     * This method is not meant to be called from typical application code.
     */
    fun removeDependent(dependent: Dependent)
}
