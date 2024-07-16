package world.phantasmal.webui.widgets

import org.w3c.dom.Node
import world.phantasmal.cell.Cell
import world.phantasmal.cell.falseCell
import world.phantasmal.cell.list.mutableListCell
import world.phantasmal.cell.mutableCell
import world.phantasmal.cell.trueCell
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.test.WebuiTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WidgetTests : WebuiTestSuite {
    @Test
    fun a_widget_disposes_its_child_widgets() = test {
        var childrenDisposed = 0

        class ChildWidget : Widget() {
            override fun Node.createElement() = div()
            override fun dispose() {
                childrenDisposed++
                super.dispose()
            }
        }

        val widget = object : Widget() {
            override fun Node.createElement() = div {
                addChild(ChildWidget())
                addChild(ChildWidget())
                addChild(ChildWidget())
            }
        }

        widget.element // Ensure widgets are fully initialized.

        widget.dispose()

        assertEquals(3, childrenDisposed)
    }

    @Test
    fun ancestorVisible_updates_and_selfAndAncestorsVisibleChanged_is_called_when_visible_changes() =
        testAsync {
            val parentVisible = mutableCell(true)
            val childVisible = mutableCell(true)
            val grandChild = DummyWidget()
            val child = DummyWidget(childVisible, grandChild)
            val parent = disposer.add(DummyWidget(parentVisible, child))

            parent.element // Ensure widgets are fully initialized.

            assertTrue(parent.publicAncestorsVisible)
            assertEquals(true, parent.selfAndAncestorsVisible)
            assertTrue(child.publicAncestorsVisible)
            assertEquals(true, child.selfAndAncestorsVisible)
            assertTrue(grandChild.publicAncestorsVisible)
            assertEquals(true, grandChild.selfAndAncestorsVisible)

            parentVisible.value = false

            assertTrue(parent.publicAncestorsVisible)
            assertEquals(false, parent.selfAndAncestorsVisible)
            assertFalse(child.publicAncestorsVisible)
            assertEquals(false, child.selfAndAncestorsVisible)
            assertFalse(grandChild.publicAncestorsVisible)
            assertEquals(false, grandChild.selfAndAncestorsVisible)

            childVisible.value = false
            parentVisible.value = true

            assertTrue(parent.publicAncestorsVisible)
            assertEquals(true, parent.selfAndAncestorsVisible)
            assertTrue(child.publicAncestorsVisible)
            assertEquals(false, child.selfAndAncestorsVisible)
            assertFalse(grandChild.publicAncestorsVisible)
            assertEquals(false, grandChild.selfAndAncestorsVisible)
        }

    @Test
    fun added_child_widgets_have_ancestorVisible_and_selfOrAncestorVisible_set_correctly() =
        test {
            val parent = disposer.add(DummyWidget(visible = falseCell()))
            val child = parent.addChild(DummyWidget())

            assertTrue(parent.publicAncestorsVisible)
            assertEquals(false, parent.selfAndAncestorsVisible)
            assertFalse(child.publicAncestorsVisible)
            assertEquals(false, child.selfAndAncestorsVisible)
        }

    @Test
    fun bindChildWidgetsTo() = test {
        val list = mutableListCell("a", "b", "c")
        var childDisposals = 0

        val parent = object : Widget() {
            override fun Node.createElement() =
                div {
                    bindChildWidgetsTo(list) { item, _ ->
                        object : Widget() {
                            override fun Node.createElement() = div { textContent = item }

                            override fun dispose() {
                                childDisposals++
                                super.dispose()
                            }
                        }
                    }
                }
        }

        parent.element // Ensure widgets are fully initialized.

        assertEquals(3, parent.children.size)
        assertEquals(3, parent.element.children.length)

        assertEquals("a", parent.children[0].element.textContent)
        assertEquals("b", parent.children[1].element.textContent)
        assertEquals("c", parent.children[2].element.textContent)

        list.replaceAll(listOf("d", "e", "f"))

        assertEquals(3, childDisposals)
        assertEquals(3, parent.children.size)
        assertEquals(3, parent.element.children.length)

        assertEquals("d", parent.children[0].element.textContent)
        assertEquals("e", parent.children[1].element.textContent)
        assertEquals("f", parent.children[2].element.textContent)

        parent.dispose()

        assertEquals(6, childDisposals)
    }

    private class DummyWidget(
        visible: Cell<Boolean> = trueCell(),
        private val child: Widget? = null,
    ) : Widget(visible = visible) {
        val publicAncestorsVisible: Boolean get() = ancestorsVisible
        var selfAndAncestorsVisible: Boolean? = null
            private set

        override fun Node.createElement() =
            div {
                child?.let { addChild(it) }
            }

        fun <T : Widget> addChild(child: T): T =
            element.addChild(child)

        override fun selfAndAncestorsVisibleChanged(visible: Boolean) {
            selfAndAncestorsVisible = visible
        }
    }
}
