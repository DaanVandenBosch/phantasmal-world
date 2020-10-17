package world.phantasmal.observable.value

class DelegatingValTests : RegularValTests() {
    override fun create(): ValAndEmit<*> {
        var v = 0
        val value = DelegatingVal({ v }, { v = it })
        return ValAndEmit(value) { value.value += 2 }
    }

    override fun createBoolean(bool: Boolean): ValAndEmit<Boolean> {
        var v = bool
        val value = DelegatingVal({ v }, { v = it })
        return ValAndEmit(value) { value.value = !value.value }
    }
}
