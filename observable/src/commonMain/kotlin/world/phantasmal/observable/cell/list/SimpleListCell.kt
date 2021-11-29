package world.phantasmal.observable.cell.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.replaceAll
import world.phantasmal.core.unsafe.unsafeAssertNotNull
import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.ChangeManager
import world.phantasmal.observable.Dependency
import world.phantasmal.observable.Dependent

typealias DependenciesExtractor<E> = (element: E) -> Array<Dependency>

/**
 * @param elements The backing list for this [ListCell].
 * @param extractDependencies Extractor function called on each element in this list, changes to
 * the returned dependencies will be propagated via [ListChange.Element]s in a [ListChangeEvent]
 * event.
 */
class SimpleListCell<E>(
    override val elements: MutableList<E>,
    private val extractDependencies: DependenciesExtractor<E>? = null,
) : AbstractListCell<E>(), MutableListCell<E> {

    /**
     * Dependents of dependencies related to this list's elements. Allows us to propagate changes to
     * elements via [ListChangeEvent]s.
     */
    private val elementDependents = mutableListOf<ElementDependent>()
    private var changingElements = 0
    private var changes = mutableListOf<ListChange<E>>()

    override var value: List<E>
        get() = elementsWrapper
        set(value) {
            replaceAll(value)
        }

    override operator fun get(index: Int): E =
        elements[index]

    override operator fun set(index: Int, element: E): E {
        checkIndex(index, elements.lastIndex)
        emitMightChange()

        copyAndResetWrapper()
        val removed = elements.set(index, element)

        if (dependents.isNotEmpty() && extractDependencies != null) {
            elementDependents[index].dispose()
            elementDependents[index] = ElementDependent(index, element)
        }

        changes.add(ListChange.Structural(index, listOf(removed), listOf(element)))
        ChangeManager.changed(this)

        return removed
    }

    override fun add(element: E) {
        emitMightChange()

        val index = elements.size
        copyAndResetWrapper()
        elements.add(element)

        finalizeStructuralChange(index, emptyList(), listOf(element))
    }

    override fun add(index: Int, element: E) {
        checkIndex(index, elements.size)
        emitMightChange()

        copyAndResetWrapper()
        elements.add(index, element)

        finalizeStructuralChange(index, emptyList(), listOf(element))
    }

    override fun remove(element: E): Boolean {
        val index = elements.indexOf(element)

        return if (index != -1) {
            removeAt(index)
            true
        } else {
            false
        }
    }

    override fun removeAt(index: Int): E {
        checkIndex(index, elements.lastIndex)
        emitMightChange()

        copyAndResetWrapper()
        val removed = elements.removeAt(index)

        finalizeStructuralChange(index, listOf(removed), emptyList())
        return removed
    }

    override fun replaceAll(elements: Iterable<E>) {
        emitMightChange()

        val removed = elementsWrapper
        copyAndResetWrapper()
        this.elements.replaceAll(elements)

        finalizeStructuralChange(0, removed, elementsWrapper)
    }

    override fun replaceAll(elements: Sequence<E>) {
        emitMightChange()

        val removed = elementsWrapper
        copyAndResetWrapper()
        this.elements.replaceAll(elements)

        finalizeStructuralChange(0, removed, elementsWrapper)
    }

    override fun splice(fromIndex: Int, removeCount: Int, newElement: E) {
        val removed = ArrayList<E>(removeCount)

        for (i in fromIndex until (fromIndex + removeCount)) {
            removed.add(elements[i])
        }

        emitMightChange()

        copyAndResetWrapper()
        repeat(removeCount) { elements.removeAt(fromIndex) }
        elements.add(fromIndex, newElement)

        finalizeStructuralChange(fromIndex, removed, listOf(newElement))
    }

    override fun clear() {
        emitMightChange()

        val removed = elementsWrapper
        copyAndResetWrapper()
        elements.clear()

        finalizeStructuralChange(0, removed, emptyList())
    }

    override fun sortWith(comparator: Comparator<E>) {
        emitMightChange()

        val removed = elementsWrapper
        copyAndResetWrapper()
        var throwable: Throwable? = null

        try {
            elements.sortWith(comparator)
        } catch (e: Throwable) {
            throwable = e
        }

        finalizeStructuralChange(0, removed, elementsWrapper)

        if (throwable != null) {
            throw throwable
        }
    }

    override fun addDependent(dependent: Dependent) {
        if (dependents.isEmpty() && extractDependencies != null) {
            for ((index, element) in elements.withIndex()) {
                elementDependents.add(ElementDependent(index, element))
            }
        }

        super.addDependent(dependent)
    }

    override fun removeDependent(dependent: Dependent) {
        super.removeDependent(dependent)

        if (dependents.isEmpty()) {
            for (elementDependent in elementDependents) {
                elementDependent.dispose()
            }

            elementDependents.clear()
        }
    }

    override fun emitDependencyChanged() {
        val currentChanges = changes
        changes = mutableListOf()
        emitDependencyChanged(ListChangeEvent(elementsWrapper, currentChanges))
    }

    private fun checkIndex(index: Int, maxIndex: Int) {
        if (index !in 0..maxIndex) {
            throw IndexOutOfBoundsException(
                "Index $index out of bounds for length ${elements.size}",
            )
        }
    }

    private fun finalizeStructuralChange(index: Int, removed: List<E>, inserted: List<E>) {
        if (dependents.isNotEmpty() && extractDependencies != null) {
            repeat(removed.size) {
                elementDependents.removeAt(index).dispose()
            }

            for ((i, element) in inserted.withIndex()) {
                val elementIdx = index + i
                elementDependents.add(elementIdx, ElementDependent(elementIdx, element))
            }

            val shift = inserted.size - removed.size

            for (i in (index + inserted.size)..elementDependents.lastIndex) {
                elementDependents[i].index += shift
            }
        }

        changes.add(ListChange.Structural(index, removed, inserted))
        ChangeManager.changed(this)
    }

    private inner class ElementDependent(
        var index: Int,
        private val element: E,
    ) : Dependent, Disposable {
        private val dependencies = unsafeAssertNotNull(extractDependencies)(element)
        private var changingDependencies = 0
        private var dependenciesActuallyChanged = false

        init {
            for (dependency in dependencies) {
                dependency.addDependent(this)
            }
        }

        override fun dispose() {
            for (dependency in dependencies) {
                dependency.removeDependent(this)
            }
        }

        override fun dependencyMightChange() {
            if (changingDependencies++ == 0) {
                changingElements++
                emitMightChange()
            }
        }

        override fun dependencyChanged(dependency: Dependency, event: ChangeEvent<*>?) {
            if (event != null) {
                dependenciesActuallyChanged = true
            }

            if (--changingDependencies == 0) {
                if (dependenciesActuallyChanged) {
                    dependenciesActuallyChanged = false
                    changes.add(ListChange.Element(index, element))
                }

                if (--changingElements == 0) {
                    ChangeManager.changed(this@SimpleListCell)
                }
            }
        }
    }
}
