package world.phantasmal.cell

@Suppress("unused")
class DelegatingCellTests : RegularCellTests, MutableCellTests<Int> {
    override fun createProvider() = object : MutableCellTests.Provider<Int> {
        private var v = 17

        override val cell = DelegatingCell({ v }, { v = it })

        override fun emit() {
            cell.value += 2
        }

        override fun createValue(): Int = v + 1
    }

    override fun <T> createWithValue(value: T): Cell<T> {
        var v = value
        return DelegatingCell({ v }, { v = it })
    }
}
