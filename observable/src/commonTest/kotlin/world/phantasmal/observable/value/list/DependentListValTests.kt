package world.phantasmal.observable.value.list

class DependentListValTests : ListValTests {
    override fun createProvider() = object : ListValTests.Provider {
        private val l = SimpleListVal<Int>(mutableListOf())

        override val observable = DependentListVal(listOf(l)) { l.value.map { 2 * it } }

        override fun addElement() {
            l.add(4)
        }
    }
}
