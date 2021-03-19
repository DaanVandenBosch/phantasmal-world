package world.phantasmal.observable

class SimpleEmitterTests : ObservableTests {
    override fun createProvider() = object : ObservableTests.Provider {
        override val observable = SimpleEmitter<Any>()

        override fun emit() {
            observable.emit(ChangeEvent(Any()))
        }
    }
}
