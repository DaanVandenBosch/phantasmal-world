package world.phantasmal.webui.widgets

import org.w3c.dom.Node
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal
import world.phantasmal.observable.value.list.mutableListVal
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.observable.value.trueVal
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.test.WebuiTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WidgetTests : WebuiTestSuite() {
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
    fun ancestorVisible_and_selfOrAncestorVisible_update_when_visible_changes() = test {
        val parentVisible = mutableVal(true)
        val childVisible = mutableVal(true)
        val grandChild = DummyWidget()
        val child = DummyWidget(childVisible, grandChild)
        val parent = disposer.add(DummyWidget(parentVisible, child))

        parent.element // Ensure widgets are fully initialized.

        assertTrue(parent.ancestorVisible.value)
        assertTrue(parent.selfOrAncestorVisible.value)
        assertTrue(child.ancestorVisible.value)
        assertTrue(child.selfOrAncestorVisible.value)
        assertTrue(grandChild.ancestorVisible.value)
        assertTrue(grandChild.selfOrAncestorVisible.value)

        parentVisible.value = false

        assertTrue(parent.ancestorVisible.value)
        assertFalse(parent.selfOrAncestorVisible.value)
        assertFalse(child.ancestorVisible.value)
        assertFalse(child.selfOrAncestorVisible.value)
        assertFalse(grandChild.ancestorVisible.value)
        assertFalse(grandChild.selfOrAncestorVisible.value)

        childVisible.value = false
        parentVisible.value = true

        assertTrue(parent.ancestorVisible.value)
        assertTrue(parent.selfOrAncestorVisible.value)
        assertTrue(child.ancestorVisible.value)
        assertFalse(child.selfOrAncestorVisible.value)
        assertFalse(grandChild.ancestorVisible.value)
        assertFalse(grandChild.selfOrAncestorVisible.value)
    }

    @Test
    fun added_child_widgets_have_ancestorVisible_and_selfOrAncestorVisible_set_correctly() =
        test {
            val parent = disposer.add(DummyWidget(visible = falseVal()))
            val child = parent.addChild(DummyWidget())

            assertTrue(parent.ancestorVisible.value)
            assertFalse(parent.selfOrAncestorVisible.value)
            assertFalse(child.ancestorVisible.value)
            assertFalse(child.selfOrAncestorVisible.value)
        }

    @Test
    fun bindChildWidgetsTo() = test {
        val list = mutableListVal("a", "b", "c")
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
        visible: Val<Boolean> = trueVal(),
        private val child: Widget? = null,
    ) : Widget(visible = visible) {
        override fun Node.createElement() =
            div {
                child?.let { addChild(it) }
            }

        fun <T : Widget> addChild(child: T): T =
            element.addChild(child)
    }
}
