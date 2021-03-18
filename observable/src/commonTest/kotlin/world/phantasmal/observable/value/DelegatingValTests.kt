package world.phantasmal.observable.value

class DelegatingValTests : RegularValTests() {
    override fun create() = object : ValAndEmit {
        private var v = 0

        override val observable = DelegatingVal({ v }, { v = it })

        override fun emit() {
            observable.value += 2
        }
    }

    override fun <T> createWithValue(value: T): DelegatingVal<T> {
        var v = value
        return DelegatingVal({ v }, { v = it })
    }
}
