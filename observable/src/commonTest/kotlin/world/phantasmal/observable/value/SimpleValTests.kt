package world.phantasmal.observable.value

class SimpleValTests : RegularValTests() {
    override fun create(): ValAndEmit<*> {
        val value = SimpleVal(1)
        return ValAndEmit(value) { value.value += 2 }
    }

    override fun createBoolean(bool: Boolean): ValAndEmit<Boolean> {
        val value = SimpleVal(bool)
        return ValAndEmit(value) { value.value = !value.value }
    }
}
