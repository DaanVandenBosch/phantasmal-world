package world.phantasmal.observable.value

class DelegatingValTests : RegularValTests, MutableValTests<Int> {
    override fun createProvider() = object : MutableValTests.Provider<Int> {
        private var v = 0

        override val observable = DelegatingVal({ v }, { v = it })

        override fun emit() {
            observable.value += 2
        }

        override fun createValue(): Int = v + 1
    }

    override fun <T> createWithValue(value: T): DelegatingVal<T> {
        var v = value
        return DelegatingVal({ v }, { v = it })
    }
}
