package world.phantasmal.observable.value.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.disposable
import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.Observable
import world.phantasmal.observable.Observer
import world.phantasmal.observable.value.AbstractVal

abstract class AbstractListVal<E>(
    protected val elements: MutableList<E>,
    private val extractObservables: ObservablesExtractor<E>?,
) : AbstractVal<List<E>>(), ListVal<E> {
    /**
     * Internal observers which observe observables related to this list's elements so that their
     * changes can be propagated via ElementChange events.
     */
    private val elementObservers = mutableListOf<ElementObserver>()

    /**
     * External list observers which are observing this list.
     */
    protected val listObservers = mutableListOf<ListValObserver<E>>()

    override fun get(index: Int): E =
        elements[index]

    override fun observe(callNow: Boolean, observer: Observer<List<E>>): Disposable {
        if (elementObservers.isEmpty() && extractObservables != null) {
            replaceElementObservers(0, elementObservers.size, elements)
        }

        observers.add(observer)

        if (callNow) {
            observer(ChangeEvent(elements))
        }

        return disposable {
            observers.remove(observer)
            disposeElementObserversIfNecessary()
        }
    }

    override fun observeList(callNow: Boolean, observer: ListValObserver<E>): Disposable {
        if (elementObservers.isEmpty() && extractObservables != null) {
            replaceElementObservers(0, elementObservers.size, elements)
        }

        listObservers.add(observer)

        if (callNow) {
            observer(ListValChangeEvent.Change(0, emptyList(), elements))
        }

        return disposable {
            listObservers.remove(observer)
            disposeElementObserversIfNecessary()
        }
    }

    /**
     * Does the following in the given order:
     * - Updates element observers
     * - Emits ListValChangeEvent
     * - Emits ValChangeEvent
     */
    protected open fun finalizeUpdate(event: ListValChangeEvent<E>) {
        if (
            (listObservers.isNotEmpty() || observers.isNotEmpty()) &&
            extractObservables != null &&
            event is ListValChangeEvent.Change
        ) {
            replaceElementObservers(event.index, event.removed.size, event.inserted)
        }

        listObservers.forEach { observer: ListValObserver<E> ->
            observer(event)
        }

        emit()
    }

    private fun replaceElementObservers(from: Int, amountRemoved: Int, insertedElements: List<E>) {
        for (i in 1..amountRemoved) {
            elementObservers.removeAt(from).observers.forEach { it.dispose() }
        }

        var index = from

        elementObservers.addAll(
            from,
            insertedElements.map { element ->
                ElementObserver(
                    index++,
                    element,
                    extractObservables!!(element)
                )
            }
        )

        val shift = insertedElements.size - amountRemoved

        while (index < elementObservers.size) {
            elementObservers[index++].index += shift
        }
    }

    private fun disposeElementObserversIfNecessary() {
        if (listObservers.isEmpty() && observers.isEmpty()) {
            elementObservers.forEach { elementObserver: ElementObserver ->
                elementObserver.observers.forEach { it.dispose() }
            }

            elementObservers.clear()
        }
    }

    private inner class ElementObserver(
        var index: Int,
        element: E,
        observables: Array<Observable<*>>,
    ) {
        val observers: Array<Disposable> = Array(observables.size) {
            observables[it].observe {
                finalizeUpdate(
                    ListValChangeEvent.ElementChange(
                        index,
                        listOf(element)
                    )
                )
            }
        }
    }
}
