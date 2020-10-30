package world.phantasmal.observable.value.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.disposable
import world.phantasmal.observable.Observable
import world.phantasmal.observable.Observer
import world.phantasmal.observable.value.*

typealias ObservablesExtractor<E> = (element: E) -> Array<Observable<*>>

class SimpleListVal<E>(
    private val elements: MutableList<E>,
    /**
     * Extractor function called on each element in this list. Changes to the returned observables
     * will be propagated via ElementChange events.
     */
    private val extractObservables: ObservablesExtractor<E>? = null,
) : MutableListVal<E> {
    override var value: List<E> = elements
        set(value) {
            val removed = ArrayList(elements)
            elements.clear()
            elements.addAll(value)
            finalizeUpdate(
                ListValChangeEvent.Change(
                    index = 0,
                    removed = removed,
                    inserted = value
                )
            )
        }

    private val mutableSizeVal: MutableVal<Int> = mutableVal(elements.size)

    override val sizeVal: Val<Int> = mutableSizeVal

    /**
     * Internal observers which observe observables related to this list's elements so that their
     * changes can be propagated via ElementChange events.
     */
    private val elementObservers = mutableListOf<ElementObserver>()

    /**
     * External list observers which are observing this list.
     */
    private val listObservers = mutableListOf<ListValObserver<E>>()

    /**
     * External regular observers which are observing this list.
     */
    private val observers = mutableListOf<ValObserver<List<E>>>()

    override fun set(index: Int, element: E): E {
        val removed = elements.set(index, element)
        finalizeUpdate(ListValChangeEvent.Change(index, listOf(removed), listOf(element)))
        return removed
    }

    override fun add(element: E) {
        val index = elements.size
        elements.add(element)
        finalizeUpdate(ListValChangeEvent.Change(index, emptyList(), listOf(element)))
    }

    override fun add(index: Int, element: E) {
        elements.add(index, element)
        finalizeUpdate(ListValChangeEvent.Change(index, emptyList(), listOf(element)))
    }

    override fun removeAt(index: Int): E {
        val removed = elements.removeAt(index)
        finalizeUpdate(ListValChangeEvent.Change(index, listOf(removed), emptyList()))
        return removed
    }

    override fun replaceAll(elements: Sequence<E>) {
        val removed = ArrayList(this.elements)
        this.elements.clear()
        this.elements.addAll(elements)
        finalizeUpdate(ListValChangeEvent.Change(0, removed, this.elements))
    }

    override fun clear() {
        val removed = ArrayList(elements)
        elements.clear()
        finalizeUpdate(ListValChangeEvent.Change(0, removed, emptyList()))
    }

    override fun observe(observer: Observer<List<E>>): Disposable =
        observe(callNow = false, observer)

    override fun observe(callNow: Boolean, observer: ValObserver<List<E>>): Disposable {
        if (elementObservers.isEmpty() && extractObservables != null) {
            replaceElementObservers(0, elementObservers.size, elements)
        }

        observers.add(observer)

        if (callNow) {
            observer(ValChangeEvent(value, value))
        }

        return disposable {
            observers.remove(observer)
            disposeElementObserversIfNecessary()
        }
    }

    override fun observeList(observer: ListValObserver<E>): Disposable {
        if (elementObservers.isEmpty() && extractObservables != null) {
            replaceElementObservers(0, elementObservers.size, elements)
        }

        listObservers.add(observer)

        return disposable {
            listObservers.remove(observer)
            disposeElementObserversIfNecessary()
        }
    }

    /**
     * Does the following in the given order:
     * - Updates element observers
     * - Emits size ValChangeEvent if necessary
     * - Emits ListValChangeEvent
     * - Emits ValChangeEvent
     */
    private fun finalizeUpdate(event: ListValChangeEvent<E>) {
        if (
            (listObservers.isNotEmpty() || observers.isNotEmpty()) &&
            extractObservables != null &&
            event is ListValChangeEvent.Change
        ) {
            replaceElementObservers(event.index, event.removed.size, event.inserted)
        }

        mutableSizeVal.value = elements.size

        listObservers.forEach { observer: ListValObserver<E> ->
            observer(event)
        }

        val regularEvent = ValChangeEvent(value, value)

        observers.forEach { observer: ValObserver<List<E>> ->
            observer(regularEvent)
        }
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
