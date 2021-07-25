package world.phantasmal.psolib.vm

import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.asm.AnyType
import world.phantasmal.psolib.buffer.Buffer

class StackFrame(
    val idx: Int,
    /**
     * The next instruction to execute.
     */
    var instructionPointer: InstructionReference,
)

class Thread {
    private val _callStack: MutableList<StackFrame> = mutableListOf()
    private val argStack: ArgStack = ArgStack()

    val callStack: List<StackFrame> = _callStack

    /**
     * Unique thread ID.
     */
    val id: Int = nextId++

    /**
     * The next instruction to execute.
     */
    val instructionPointer: InstructionReference?
        get() = currentStackFrame()?.instructionPointer

    /**
     * A thread is live as long as there is at least one frame on the stack.
     */
    val live: Boolean = callStack.isNotEmpty()

    /**
     * Returns null when the thread has been terminated.
     */
    fun currentStackFrame(): StackFrame? = callStack.lastOrNull()

    /**
     * Push a new frame onto the call stack.
     */
    fun pushFrame(instructionPointer: InstructionReference) {
        _callStack.add(StackFrame(callStack.size, instructionPointer))
    }

    /**
     * Pops the current frame from the call stack.
     */
    fun popFrame() {
        _callStack.removeLast()
    }

    companion object {
        private var nextId = 0

        fun resetIdCounter() {
            nextId = 0
        }
    }
}

private class ArgStack {
    private val stack: Buffer =
        Buffer.withCapacity(MAX_SIZE * SLOT_SIZE, Endianness.Little)

    private val types: MutableList<AnyType> = mutableListOf()

    /**
     * Returns false on overflow.
     */
    fun push(value: Int, type: AnyType): Boolean {
        if (stack.size >= MAX_SIZE) {
            return false
        }

        val idx = stack.size
        stack.size += SLOT_SIZE
        stack.setInt(idx, value)
        types.add(type)

        return true
    }

    companion object {
        private const val MAX_SIZE = 8
        private const val SLOT_SIZE = 4
    }
}
