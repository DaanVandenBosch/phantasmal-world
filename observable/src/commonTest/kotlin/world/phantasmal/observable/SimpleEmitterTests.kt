package world.phantasmal.observable

import world.phantasmal.observable.test.TestSuite
import kotlin.test.Test

class SimpleEmitterTests : TestSuite() {
    @Test
    fun observable_tests() {
        observableTests {
            val observable = SimpleEmitter<Any>()
            ObservableAndEmit(observable) { observable.emit(ChangeEvent(Any())) }
        }
    }
}
