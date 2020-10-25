package world.phantasmal.observable.value

/**
 * In these tests the dependency of the [FlatTransformedVal]'s direct dependency changes.
 */
class FlatTransformedValNestedValEmitsTests : RegularValTests() {
    override fun create(): ValAndEmit<*> {
        val v = SimpleVal(SimpleVal(5))
        val value = FlatTransformedVal(listOf(v)) { v.value }
        return ValAndEmit(value) { v.value.value += 5 }
    }

    override fun createBoolean(bool: Boolean): ValAndEmit<Boolean> {
        val v = SimpleVal(SimpleVal(bool))
        val value = FlatTransformedVal(listOf(v)) { v.value }
        return ValAndEmit(value) { v.value.value = !v.value.value }
    }
}
