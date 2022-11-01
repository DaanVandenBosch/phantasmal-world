package world.phantasmal.cell

/**
 * This interface is not meant to be implemented by typical application code.
 */
interface Dependency<out T> {
    /**
     * This property is not meant to be accessed from typical application code. The current change
     * event for this dependency. Only valid during a mutation.
     */
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
