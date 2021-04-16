package world.phantasmal.observable.value

class DependentValTests : RegularValTests {
    override fun createProvider() = object : ValTests.Provider {
        val v = SimpleVal(0)

        override val observable = DependentVal(v) { 2 * v.value }

        override fun emit() {
            v.value += 2
        }
    }

    override fun <T> createWithValue(value: T): DependentVal<T> {
        val v = SimpleVal(value)
        return DependentVal(v) { v.value }
    }
}
