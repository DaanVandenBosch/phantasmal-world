package world.phantasmal.observable.value.list

class SimpleListValTests : ListValTests() {
    override fun create(): ListValAndAdd<*, SimpleListVal<*>> {
        val value = SimpleListVal(mutableListOf<Int>())
        return ListValAndAdd(value) { value.add(7) }
    }
}
