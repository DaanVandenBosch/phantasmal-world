package world.phantasmal.observable.value

class MappedValTests : RegularValTests() {
    override fun create(): ValAndEmit<*> {
        val v = SimpleVal(0)
        val value = MappedVal(listOf(v)) { 2 * v.value }
        return ValAndEmit(value) { v.value += 2 }
    }

    override fun createBoolean(bool: Boolean): ValAndEmit<Boolean> {
        val v = SimpleVal(bool)
        val value = MappedVal(listOf(v)) { v.value }
        return ValAndEmit(value) { v.value = !v.value }
    }
}
