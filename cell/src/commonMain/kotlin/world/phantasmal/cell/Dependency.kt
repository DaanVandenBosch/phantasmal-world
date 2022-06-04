package world.phantasmal.cell

interface Dependency<out T> {
    // TODO: Docs.
    val changeEvent: ChangeEvent<T>?

    /**
     * This method is not meant to be called from typical application code. Usually you'll want to
     * use [Cell.observeChange].
     */
    fun addDependent(dependent: Dependent)

    /**
     * This method is not meant to be called from typical application code.
     */
    fun removeDependent(dependent: Dependent)
}
