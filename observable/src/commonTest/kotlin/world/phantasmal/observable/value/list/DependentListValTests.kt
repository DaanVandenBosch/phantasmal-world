package world.phantasmal.observable.value.list

class DependentListValTests : ListValTests() {
    override fun create(): ListValAndAdd<*, DependentListVal<*>> {
        val l = SimpleListVal<Int>(mutableListOf())
        val list = DependentListVal(listOf(l)) { l.value.map { 2 * it } }
        return ListValAndAdd(list) { l.add(4) }
    }
}
