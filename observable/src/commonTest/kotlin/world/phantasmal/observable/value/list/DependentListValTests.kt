package world.phantasmal.observable.value.list

class DependentListValTests : ListValTests() {
    override fun create() = object : ListValAndAdd {
        private val l = SimpleListVal<Int>(mutableListOf())

        override val observable = DependentListVal(listOf(l)) { l.value.map { 2 * it } }

        override fun add() {
            l.add(4)
        }
    }
}
