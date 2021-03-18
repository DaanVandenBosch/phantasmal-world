package world.phantasmal.observable.value

class SimpleValTests : RegularValTests() {
    override fun create() = object : ValAndEmit {
        override val observable = SimpleVal(1)

        override fun emit() {
            observable.value += 2
        }
    }

    override fun <T> createWithValue(value: T) = SimpleVal(value)
}
