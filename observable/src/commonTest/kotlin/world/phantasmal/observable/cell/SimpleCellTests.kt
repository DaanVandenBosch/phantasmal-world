package world.phantasmal.observable.cell

class SimpleCellTests : RegularCellTests, MutableCellTests<Int> {
    override fun createProvider() = object : MutableCellTests.Provider<Int> {
        override val observable = SimpleCell(1)

        override fun emit() {
            observable.value += 2
        }

        override fun createValue(): Int = observable.value + 1
    }

    override fun <T> createWithValue(value: T) = SimpleCell(value)
}
