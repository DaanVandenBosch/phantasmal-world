package world.phantasmal.observable.cell.list

import world.phantasmal.core.disposable.Disposable
import world.phantasmal.core.disposable.disposable
import world.phantasmal.core.unsafe.unsafeAssertNotNull
import world.phantasmal.observable.ChangeEvent
import world.phantasmal.observable.Observable
import world.phantasmal.observable.Observer
import world.phantasmal.observable.cell.AbstractCell
import world.phantasmal.observable.cell.Cell
import world.phantasmal.observable.cell.DependentCell
import world.phantasmal.observable.cell.not

abstract class AbstractListCell<E>(
    private val extractObservables: ObservablesExtractor<E>?,
) : AbstractCell<List<E>>(), ListCell<E> {
    /**
     * Internal observers which observe observables related to this list's elements so that their
     * changes can be propagated via ElementChange events.
     */
    private val elementObservers = mutableListOf<ElementObserver>()

    /**
     * External list observers which are observing this list.
     */
    protected val listObservers = mutableListOf<ListObserver<E>>()

    override val empty: Cell<Boolean> by lazy { size.map { it == 0 } }

    override val notEmpty: Cell<Boolean> by lazy { !empty }

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

    override fun observeList(callNow: Boolean, observer: ListObserver<E>): Disposable {
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

    override fun firstOrNull(): Cell<E?> =
        DependentCell(this) { value.firstOrNull() }

    /**
     * Does the following in the given order:
     * - Updates element observers
     * - Emits ListChangeEvent
     * - Emits ChangeEvent
     */
    protected open fun finalizeUpdate(event: ListChangeEvent<E>) {
        if (
            (listObservers.isNotEmpty() || observers.isNotEmpty()) &&
            extractObservables != null &&
            event is ListChangeEvent.Change
        ) {
            replaceElementObservers(event.index, event.removed.size, event.inserted)
        }

        listObservers.forEach { observer: ListObserver<E> ->
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
