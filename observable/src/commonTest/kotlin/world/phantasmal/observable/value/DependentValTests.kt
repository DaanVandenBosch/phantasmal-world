package world.phantasmal.observable.value

class DependentValTests : RegularValTests() {
    override fun create() = object : ValAndEmit {
        val v = SimpleVal(0)

        override val observable = DependentVal(listOf(v)) { 2 * v.value }

        override fun emit() {
            v.value += 2
        }
    }

    override fun <T> createWithValue(value: T): DependentVal<T> {
        val v = SimpleVal(value)
        return DependentVal(listOf(v)) { v.value }
    }
}
