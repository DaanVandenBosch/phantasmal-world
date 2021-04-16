package world.phantasmal.observable.value.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.disposable
import world.phantasmal.core.unsafeAssertNotNull
import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.Observable
import world.phantasmal.observable.Observer
import world.phantasmal.observable.value.AbstractVal
import world.phantasmal.observable.value.DependentVal
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.not

abstract class AbstractListVal<E>(
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

    override val empty: Val<Boolean> by lazy { size.map { it == 0 } }

    override val notEmpty: Val<Boolean> by lazy { !empty }

    override fun get(index: Int): E =
        value[index]

    override fun observe(callNow: Boolean, observer: Observer<List<E>>): Disposable {
        if (elementObservers.isEmpty() && extractObservables != null) {
            replaceElementObservers(0, elementObservers.size, value)
        }

        observers.add(observer)

        if (callNow) {
            observer(ChangeEvent(value))
        }

        return disposable {
            observers.remove(observer)
            disposeElementObserversIfNecessary()
        }
    }

    override fun observeList(callNow: Boolean, observer: ListValObserver<E>): Disposable {
        if (elementObservers.isEmpty() && extractObservables != null) {
            replaceElementObservers(0, elementObservers.size, value)
        }

        listObservers.add(observer)

        if (callNow) {
            observer(ListChangeEvent.Change(0, emptyList(), value))
        }

        return disposable {
            listObservers.remove(observer)
            disposeElementObserversIfNecessary()
        }
    }

    override fun firstOrNull(): Val<E?> =
        DependentVal(this) { value.firstOrNull() }

    /**
     * Does the following in the given order:
     * - Updates element observers
     * - Emits ListValChangeEvent
     * - Emits ValChangeEvent
     */
    protected open fun finalizeUpdate(event: ListChangeEvent<E>) {
        if (
            (listObservers.isNotEmpty() || observers.isNotEmpty()) &&
            extractObservables != null &&
            event is ListChangeEvent.Change
        ) {
            replaceElementObservers(event.index, event.removed.size, event.inserted)
        }

        listObservers.forEach { observer: ListValObserver<E> ->
            observer(event)
        }

        emit()
    }

    private fun replaceElementObservers(from: Int, amountRemoved: Int, insertedElements: List<E>) {
        repeat(amountRemoved) {
            elementObservers.removeAt(from).observers.forEach { it.dispose() }
        }

        var index = from

        elementObservers.addAll(
            from,
            insertedElements.map { element ->
                ElementObserver(
                    index++,
                    element,
                    extractObservables.unsafeAssertNotNull()(element)
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
                finalizeUpdate(ListChangeEvent.ElementChange(index, element))
            }
        }
    }
}
