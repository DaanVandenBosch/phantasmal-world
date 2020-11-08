package world.phantasmal.webui.widgets

import kotlinx.coroutines.CoroutineScope
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
    fun ancestorHidden_and_selfOrAncestorHidden_should_update_when_hidden_changes() = test {
        val parentHidden = mutableVal(false)
        val childHidden = mutableVal(false)
        val grandChild = DummyWidget(scope)
        val child = DummyWidget(scope, childHidden, grandChild)
        val parent = disposer.add(DummyWidget(scope, parentHidden, child))

        parent.element // Ensure widgets are fully initialized.

        assertFalse(parent.ancestorHidden.value)
        assertFalse(parent.selfOrAncestorHidden.value)
        assertFalse(child.ancestorHidden.value)
        assertFalse(child.selfOrAncestorHidden.value)
        assertFalse(grandChild.ancestorHidden.value)
        assertFalse(grandChild.selfOrAncestorHidden.value)

        parentHidden.value = true

        assertFalse(parent.ancestorHidden.value)
        assertTrue(parent.selfOrAncestorHidden.value)
        assertTrue(child.ancestorHidden.value)
        assertTrue(child.selfOrAncestorHidden.value)
        assertTrue(grandChild.ancestorHidden.value)
        assertTrue(grandChild.selfOrAncestorHidden.value)

        childHidden.value = true
        parentHidden.value = false

        assertFalse(parent.ancestorHidden.value)
        assertFalse(parent.selfOrAncestorHidden.value)
        assertFalse(child.ancestorHidden.value)
        assertTrue(child.selfOrAncestorHidden.value)
        assertTrue(grandChild.ancestorHidden.value)
        assertTrue(grandChild.selfOrAncestorHidden.value)
    }

    @Test
    fun added_child_widgets_should_have_ancestorHidden_and_selfOrAncestorHidden_set_correctly() =
        test {
            val parent = disposer.add(DummyWidget(scope, hidden = trueVal()))
            val child = parent.addChild(DummyWidget(scope))

            assertFalse(parent.ancestorHidden.value)
            assertTrue(parent.selfOrAncestorHidden.value)
            assertTrue(child.ancestorHidden.value)
            assertTrue(child.selfOrAncestorHidden.value)
        }

    private inner class DummyWidget(
        scope: CoroutineScope,
        hidden: Val<Boolean> = falseVal(),
        private val child: Widget? = null,
    ) : Widget(scope, hidden = hidden) {
        override fun Node.createElement() = div {
            child?.let { addChild(it) }
        }

        fun <T : Widget> addChild(child: T): T =
            element.addChild(child)
    }
}
