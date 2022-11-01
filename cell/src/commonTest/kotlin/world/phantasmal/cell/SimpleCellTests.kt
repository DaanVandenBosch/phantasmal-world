package world.phantasmal.cell

@Suppress("unused")
class SimpleCellTests : RegularCellTests, MutableCellTests<Int> {
    override fun createProvider() = object : MutableCellTests.Provider<Int> {
        override val cell = SimpleCell(1)

        override fun emit() {
            cell.value += 2
        }

        override fun createValue(): Int = cell.value + 1
    }

    override fun <T> createWithValue(value: T): Cell<T> = SimpleCell(value)
}
