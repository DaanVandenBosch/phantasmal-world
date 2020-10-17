package world.phantasmal.observable

class SimpleEmitterTests : ObservableTests() {
    override fun create(): ObservableAndEmit {
        val observable = SimpleEmitter<Any>()
        return ObservableAndEmit(observable) { observable.emit(ChangeEvent(Any())) }
    }
}
