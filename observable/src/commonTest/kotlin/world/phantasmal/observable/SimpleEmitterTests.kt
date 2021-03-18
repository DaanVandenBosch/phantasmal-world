package world.phantasmal.observable

class SimpleEmitterTests : ObservableTests() {
    override fun create(): ObservableAndEmit =
        object : ObservableAndEmit {
            override val observable = SimpleEmitter<Any>()

            override fun emit() {
                observable.emit(ChangeEvent(Any()))
            }
        }
}
