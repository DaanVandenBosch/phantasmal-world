package world.phantasmal.webui.widgets

import org.w3c.dom.Node
import world.phantasmal.observable.value.Val
import world.phantasmal.observable.value.falseVal
import world.phantasmal.observable.value.mutableVal
import world.phantasmal.observable.value.trueVal
import world.phantasmal.webui.dom.div
import world.phantasmal.webui.test.WebuiTestSuite
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WidgetTests : WebuiTestSuite() {
    @Test
    fun ancestorVisible_and_selfOrAncestorVisible_should_update_when_visible_changes() = test {
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
    fun added_child_widgets_should_have_ancestorVisible_and_selfOrAncestorVisible_set_correctly() =
        test {
            val parent = disposer.add(DummyWidget(visible = falseVal()))
            val child = parent.addChild(DummyWidget())

            assertTrue(parent.ancestorVisible.value)
            assertFalse(parent.selfOrAncestorVisible.value)
            assertFalse(child.ancestorVisible.value)
            assertFalse(child.selfOrAncestorVisible.value)
        }

    private inner class DummyWidget(
        visible: Val<Boolean> = trueVal(),
        private val child: Widget? = null,
    ) : Widget(visible = visible) {
        override fun Node.createElement() = div {
            child?.let { addChild(it) }
        }

        fun <T : Widget> addChild(child: T): T =
            element.addChild(child)
    }
}
