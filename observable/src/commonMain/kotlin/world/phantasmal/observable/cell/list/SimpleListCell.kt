package world.phantasmal.observable.cell.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.unsafe.unsafeAssertNotNull
import world.phantasmal.observable.ChangeEvent
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
    elements: MutableList<E>,
    private val extractDependencies: DependenciesExtractor<E>? = null,
) : AbstractListCell<E>(), MutableListCell<E> {

    private var elements = ListWrapper(elements)

    /**
     * Dependents of dependencies related to this list's elements. Allows us to propagate changes to
     * elements via [ListChangeEvent]s.
     */
    private val elementDependents = mutableListOf<ElementDependent>()
    private var changingElements = 0
    private var elementListChanges = mutableListOf<ListChange.Element<E>>()

    override var value: List<E>
        get() = elements
        set(value) {
            replaceAll(value)
        }

    override operator fun get(index: Int): E =
        elements[index]

    override operator fun set(index: Int, element: E): E {
        checkIndex(index, elements.lastIndex)
        emitMightChange()

        val removed: E
        elements = elements.mutate { removed = set(index, element) }

        if (extractDependencies != null) {
            elementDependents[index].dispose()
            elementDependents[index] = ElementDependent(index, element)
        }

        emitChanged(
            ListChangeEvent(
                elements,
                listOf(ListChange.Structural(index, listOf(removed), listOf(element))),
            ),
        )

        return removed
    }

    override fun add(element: E) {
        emitMightChange()

        val index = elements.size
        elements = elements.mutate { add(index, element) }

        finalizeStructuralChange(index, emptyList(), listOf(element))
    }

    override fun add(index: Int, element: E) {
        checkIndex(index, elements.size)
        emitMightChange()

        elements = elements.mutate { add(index, element) }

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

        val removed: E
        elements = elements.mutate { removed = removeAt(index) }

        finalizeStructuralChange(index, listOf(removed), emptyList())
        return removed
    }

    override fun replaceAll(elements: Iterable<E>) {
        emitMightChange()

        val removed = this.elements
        this.elements = ListWrapper(elements.toMutableList())

        finalizeStructuralChange(0, removed, this.elements)
    }

    override fun replaceAll(elements: Sequence<E>) {
        emitMightChange()

        val removed = this.elements
        this.elements = ListWrapper(elements.toMutableList())

        finalizeStructuralChange(0, removed, this.elements)
    }

    override fun splice(fromIndex: Int, removeCount: Int, newElement: E) {
        val removed = ArrayList(elements.subList(fromIndex, fromIndex + removeCount))

        emitMightChange()

        elements = elements.mutate {
            repeat(removeCount) { removeAt(fromIndex) }
            add(fromIndex, newElement)
        }

        finalizeStructuralChange(fromIndex, removed, listOf(newElement))
    }

    override fun clear() {
        emitMightChange()

        val removed = elements
        elements = ListWrapper(mutableListOf())

        finalizeStructuralChange(0, removed, emptyList())
    }

    override fun sortWith(comparator: Comparator<E>) {
        emitMightChange()

        var throwable: Throwable? = null

        try {
            elements = elements.mutate { sortWith(comparator) }
        } catch (e: Throwable) {
            throwable = e
        }

        finalizeStructuralChange(0, elements, elements)

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

    private fun checkIndex(index: Int, maxIndex: Int) {
        if (index !in 0..maxIndex) {
            throw IndexOutOfBoundsException(
                "Index $index out of bounds for length ${elements.size}",
            )
        }
    }

    private fun finalizeStructuralChange(index: Int, removed: List<E>, inserted: List<E>) {
        if (extractDependencies != null) {
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

        emitChanged(
            ListChangeEvent(
                elements,
                listOf(ListChange.Structural(index, removed, inserted)),
            ),
        )
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
                    elementListChanges.add(ListChange.Element(index, element))
                }

                if (--changingElements == 0) {
                    try {
                        if (elementListChanges.isNotEmpty()) {
                            emitChanged(ListChangeEvent(value, elementListChanges))
                        } else {
                            emitChanged(null)
                        }
                    } finally {
                        elementListChanges = mutableListOf()
                    }
                }
            }
        }
    }
}
