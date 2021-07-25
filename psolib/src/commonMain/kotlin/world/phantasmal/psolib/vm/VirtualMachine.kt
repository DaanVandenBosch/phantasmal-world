package world.phantasmal.psolib.vm

import mu.KotlinLogging
import world.phantasmal.psolib.Endianness
import world.phantasmal.psolib.Episode
import world.phantasmal.psolib.asm.*
import world.phantasmal.psolib.buffer.Buffer

private val logger = KotlinLogging.logger {}

enum class ExecutionResult {
    /**
     * There are no live threads, nothing to do.
     */
    Suspended,

    /**
     * Execution is paused because of the [ExecutionInterceptor].
     */
    Paused,

    /**
     * All threads have yielded.
     */
    WaitingVsync,

    /**
     * Execution has halted because the VM encountered an `exit` instruction, a fatal error was
     * raised or the VM was halted from outside.
     */
    Halted,
}

interface ExecutionInterceptor {
    /**
     * Called before the VM executes an instruction. If false is returned, the VM won't execute the
     * instruction and pause.
     */
    fun intercept(instruction: InstructionReference): Boolean
}

/**
 * This class emulates the PSO script engine. It's in charge of memory, threading and executing
 * instructions.
 */
class VirtualMachine {
    // Quest Details

    private var episode: Episode = Episode.I

    var bytecode: BytecodeIr = BytecodeIr(emptyList())
        private set

    private val labelToSegIdx: MutableMap<Int, Int> = mutableMapOf()

    // VM State

    private val registers = Buffer.withSize(REGISTER_COUNT * REGISTER_SIZE, Endianness.Little)
    private var stringArgStore = ""

    /**
     * All live threads.
     */
    private val threads: MutableList<Thread> = mutableListOf()

    /**
     * Current thread index into [threads].
     */
    private var threadIdx = 0

    var halted = true
        private set

    var executionInterceptor: ExecutionInterceptor? = null

    /**
     * Halts and resets the VM, then loads new bytecode.
     */
    fun loadBytecode(bytecode: BytecodeIr, episode: Episode) {
        halt()

        logger.debug { "Starting." }

        this.bytecode = bytecode
        this.episode = episode

        labelToSegIdx.clear()

        for ((segIdx, segment) in bytecode.segments.withIndex()) {
            for (label in segment.labels) {
                labelToSegIdx[label] = segIdx
            }
        }

        halted = false
    }

    /**
     * Executes instructions while possible.
     */
    fun execute(): ExecutionResult {
        require(!halted) { "Halted." }
        require(threads.isNotEmpty()) { "Suspended." }
        require(threadIdx < threads.size) { "Awaiting vsync." }

        try {
            // Limit amount of instructions executed to prevent infinite loops.
            var executionCounter = 0

            while (executionCounter++ < 10_000) {
                // Execute the instruction pointed to by the current thread.
                val thread = currentThread()!!
                val instRef = thread.instructionPointer!!

                if (executionInterceptor?.intercept(instRef) == false) {
                    return ExecutionResult.Paused
                } else {
                    val result = executeInstruction()

                    if (result != null && result != ExecutionResult.WaitingVsync) {
                        return result
                    }
                }

                if (threads.isEmpty()) {
                    return ExecutionResult.Suspended
                }

                if (threadIdx > threads.lastIndex) {
                    return ExecutionResult.WaitingVsync
                }
            }

            // TODO: Output error "Maximum execution count reached. The code probably contains an infinite loop.".
            halt()
            return ExecutionResult.Halted
        } catch (e: Throwable) {
            // TODO: Output error.
            halt()
            return ExecutionResult.Halted
        }
    }

    /**
     * Signal to the VM that a vsync has happened.
     */
    fun vsync() {
        if (threadIdx > threads.lastIndex) {
            threadIdx = 0
        }
    }

    /**
     * Halts execution of all threads.
     */
    fun halt() {
        if (!halted) {
            logger.debug { "Halting." }

            registers.zero()
            stringArgStore = ""
            threads.clear()
            threadIdx = 0
            // TODO:
//            window_msg_open = false
//            set_episode_called = false
//            list_open = false
//            selection_reg = 0
            halted = true
//            paused = false
//            breakpoints.splice(0, Infinity)
//            unsupported_opcodes_logged.clear()
            Thread.resetIdCounter()
        }
    }

    private fun executeInstruction(): ExecutionResult? {
        val thread = threads[threadIdx]
        val inst = thread.instructionPointer!!.instruction
        val args = inst.args.map { it.value }

        var advance = true
        var result: ExecutionResult? = null

        when (inst.opcode.code) {
            OP_NOP.code -> {
            }
            OP_RET.code -> {
                popFrame(threadIdx)
            }
            OP_SYNC.code -> {
                advanceInstructionPointer(threadIdx)
                threadIdx++
                advance = false
                result = ExecutionResult.WaitingVsync
            }
            OP_EXIT.code -> {
                halt()
            }
            OP_THREAD.code -> {
                TODO()
            }
            OP_CALL.code -> {
                pushFrame(thread, args[0] as Int)
                advance = false
            }
            OP_JMP.code -> {
                jumpToLabel(thread, args[0] as Int)
                advance = false
            }
        }

        if (advance) {
            advanceInstructionPointer(threadIdx)
        }

        return result
    }

    /**
     * Simply advance to the next instruction no matter what the current instruction is. I.e. this
     * method simply advances past jumps and calls.
     */
    private fun advanceInstructionPointer(threadIdx: Int) {
        val thread = threads[threadIdx]
        require(thread.live) { "Trying to advance the instruction pointer within a dead thread." }

        val frame = thread.currentStackFrame()!!
        val next = frame.instructionPointer.next()

        if (next == null) {
            // Reached EOF.
            // Game will crash if call stack is not empty.
            require(thread.callStack.isEmpty()) {
                "Reached EOF but call stack was not empty"
            }

            thread.popFrame()
            terminateThread(threadIdx)
        } else {
            frame.instructionPointer = next
        }
    }

    private fun currentThread(): Thread? = threads.getOrNull(threadIdx)

    private fun terminateThread(idx: Int) {
        val thread = threads.removeAt(idx)

        if (threadIdx >= idx && threadIdx > 0) {
            threadIdx--
        }

        logger.debug { "Thread #${thread.id} terminated." }
    }

    private fun pushFrame(thread: Thread, label: Int) {
        val segIdx = getSegmentIndexByLabel(label)
        val segment = bytecode.segments[segIdx]

        require(segment.type == SegmentType.Instructions) {
            "Label $label points to a ${segment.type} segment, expecting ${
                SegmentType.Instructions
            }."
        }

        thread.pushFrame(InstructionReference(segIdx, 0, bytecode))
    }

    private fun popFrame(threadIdx: Int) {
        val thread = threads[threadIdx]
        thread.popFrame()

        if (!thread.live) {
            terminateThread(threadIdx)
        }
    }

    private fun jumpToLabel(thread: Thread, label: Int) {
        thread.currentStackFrame()!!.instructionPointer =
            InstructionReference(getSegmentIndexByLabel(label), 0, bytecode)
    }

    private fun getSegmentIndexByLabel(label: Int): Int =
        labelToSegIdx[label] ?: error("Invalid argument: No such label $label.")

    companion object {
        private const val REGISTER_SIZE = 4

        const val REGISTER_COUNT = 256
    }
}
