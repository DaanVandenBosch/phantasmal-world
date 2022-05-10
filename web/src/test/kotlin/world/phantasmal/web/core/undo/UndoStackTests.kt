package world.phantasmal.web.core.undo

import world.phantasmal.web.core.commands.Command
import world.phantasmal.web.test.WebTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UndoStackTests : WebTestSuite {
    @Test
    fun simple_properties_and_invariants() = test {
        val stack = UndoStack(UndoManager())

        assertFalse(stack.canUndo.value)
        assertFalse(stack.canRedo.value)

        stack.push(DummyCommand())
        stack.push(DummyCommand())
        stack.push(DummyCommand())

        assertTrue(stack.canUndo.value)
        assertFalse(stack.canRedo.value)

        stack.undo()

        assertTrue(stack.canUndo.value)
        assertTrue(stack.canRedo.value)

        stack.undo()
        stack.undo()

        assertFalse(stack.canUndo.value)
        assertTrue(stack.canRedo.value)
    }

    @Test
    fun undo() = test {
        val stack = UndoStack(UndoManager())

        var value = 3

        stack.push(DummyCommand(execute = { value = 7 }, undo = { value = 3 })).execute()
        stack.push(DummyCommand(execute = { value = 13 }, undo = { value = 7 })).execute()

        assertEquals(13, value)

        assertTrue(stack.undo())
        assertEquals(7, value)

        assertTrue(stack.undo())
        assertEquals(3, value)

        assertFalse(stack.undo())
        assertEquals(3, value)
    }

    @Test
    fun redo() = test {
        val stack = UndoStack(UndoManager())

        var value = 3

        stack.push(DummyCommand(execute = { value = 7 }, undo = { value = 3 })).execute()
        stack.push(DummyCommand(execute = { value = 13 }, undo = { value = 7 })).execute()

        stack.undo()
        stack.undo()

        assertEquals(3, value)

        assertTrue(stack.redo())
        assertEquals(7, value)

        assertTrue(stack.redo())
        assertEquals(13, value)

        assertFalse(stack.redo())
        assertEquals(13, value)
    }

    @Test
    fun push_then_undo_then_push_again() = test {
        val stack = UndoStack(UndoManager())

        var value = 3

        stack.push(DummyCommand(execute = { value = 7 }, undo = { value = 3 })).execute()

        stack.undo()

        assertEquals(3, value)

        stack.push(DummyCommand(execute = { value = 13 }, undo = { value = 7 })).execute()

        assertEquals(13, value)
    }

    private class DummyCommand(
        private val execute: () -> Unit = {},
        private val undo: () -> Unit = {},
    ) : Command {
        override val description: String = "Dummy command"

        override fun execute() {
            execute.invoke()
        }

        override fun undo() {
            undo.invoke()
        }
    }
}
