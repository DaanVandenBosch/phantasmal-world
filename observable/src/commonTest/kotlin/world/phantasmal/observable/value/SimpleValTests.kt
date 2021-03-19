package world.phantasmal.observable.value

class SimpleValTests : RegularValTests, MutableValTests<Int> {
    override fun createProvider() = object : MutableValTests.Provider<Int> {
        override val observable = SimpleVal(1)

        override fun emit() {
            observable.value += 2
        }

        override fun createValue(): Int = observable.value + 1
    }

    override fun <T> createWithValue(value: T) = SimpleVal(value)
}
