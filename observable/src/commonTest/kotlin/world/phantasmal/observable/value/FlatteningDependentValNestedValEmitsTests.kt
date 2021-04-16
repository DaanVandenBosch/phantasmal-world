package world.phantasmal.observable.value

/**
 * In these tests the dependency of the [FlatteningDependentVal]'s direct dependency changes.
 */
class FlatteningDependentValNestedValEmitsTests : RegularValTests {
    override fun createProvider() = object : ValTests.Provider {
        val v = StaticVal(SimpleVal(5))

        override val observable = FlatteningDependentVal(v) { v.value }

        override fun emit() {
            v.value.value += 5
        }
    }

    override fun <T> createWithValue(value: T): FlatteningDependentVal<T> {
        val v = StaticVal(StaticVal(value))
        return FlatteningDependentVal(v) { v.value }
    }
}
