package world.phantasmal.observable.cell.list

import world.phantasmal.observable.cell.*

/**
 * In these tests, the direct list cell dependency of the [ListElementsDependentCell] doesn't
 * change, but its elements do change.
 */
class ListElementsDependentCellElementEmitsTests : CellWithDependenciesTests {

    override fun createProvider() = object : CellTests.Provider {
        // One transitive dependency can change.
        private val transitiveDependency = Element(2)

        // The direct dependency of the list under test can't change.
        private val directDependency: ListCell<Element> =
            ImmutableListCell(listOf(Element(1), transitiveDependency, Element(3)))

        override val observable =
            ListElementsDependentCell(directDependency) { arrayOf(it.int, it.double, it.string) }

        override fun emit() {
            transitiveDependency.int.value++
        }
    }

    override fun createWithDependencies(
        dependency1: Cell<Int>,
        dependency2: Cell<Int>,
        dependency3: Cell<Int>,
    ) =
        ListElementsDependentCell(
            ImmutableListCell(listOf(dependency1, dependency2, dependency3))
        ) { arrayOf(it) }

    private class Element(value: Int) {
        val int: MutableCell<Int> = SimpleCell(value)
        val double: Cell<Double> = DependentCell(int) { int.value.toDouble() }
        val string: Cell<String> = DependentCell(int) { int.value.toString() }

        override fun toString(): String = "Element[int=$int, double=$double, string=$string]"
    }
}
