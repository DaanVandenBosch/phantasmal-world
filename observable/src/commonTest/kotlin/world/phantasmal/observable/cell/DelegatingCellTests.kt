package world.phantasmal.observable.cell

class DelegatingCellTests : RegularCellTests, MutableCellTests<Int> {
    override fun createProvider() = object : MutableCellTests.Provider<Int> {
        private var v = 17

        override val observable = DelegatingCell({ v }, { v = it })

        override fun emit() {
            observable.value += 2
        }

        override fun createValue(): Int = v + 1
    }

    override fun <T> createWithValue(value: T): DelegatingCell<T> {
        var v = value
        return DelegatingCell({ v }, { v = it })
    }
}
