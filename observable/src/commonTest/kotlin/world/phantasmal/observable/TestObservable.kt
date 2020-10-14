package world.phantasmal.observable

// Test suite for all Observable implementations.
// These functions are called from type-specific unit tests.

import world.phantasmal.observable.test.withScope
import kotlin.test.assertEquals

typealias ObservableAndEmit = Pair<Observable<*>, () -> Unit>

fun observableTests(create: () -> ObservableAndEmit) {
    observableShouldCallObserversWhenEventsAreEmitted(create)
    observableShouldNotCallObserversAfterTheyAreDisposed(create)
}

private fun observableShouldCallObserversWhenEventsAreEmitted(create: () -> ObservableAndEmit) {
    val (observable, emit) = create()
    val changes = mutableListOf<ChangeEvent<*>>()

    withScope { scope ->
        observable.observe(scope) { c ->
            changes.add(c)
        }

        emit()

        assertEquals(1, changes.size)

        emit()
        emit()
        emit()

        assertEquals(4, changes.size)
    }
}

private fun observableShouldNotCallObserversAfterTheyAreDisposed(create: () -> ObservableAndEmit) {
    val (observable, emit) = create()
    val changes = mutableListOf<ChangeEvent<*>>()

    withScope { scope ->
        observable.observe(scope) { c ->
            changes.add(c)
        }

        emit()

        assertEquals(1, changes.size)

        emit()
        emit()
        emit()

        assertEquals(4, changes.size)
    }
}
