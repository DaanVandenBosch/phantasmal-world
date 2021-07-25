package world.phantasmal.psolib.asm

import world.phantasmal.core.Success
import world.phantasmal.psolib.test.LibTestSuite
import world.phantasmal.psolib.test.assertDeepEquals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AssemblyTests : LibTestSuite {
    @Test
    fun basic_script() {
        val result = assemble(
            """
            0:
                set_episode 0
                bb_map_designate 1, 2, 3, 4
                set_floor_handler 0, 150
                ret
            150:
                set_mainwarp 1
                ret
            """.trimIndent().split('\n')
        )

        assertTrue(result is Success)
        assertTrue(result.problems.isEmpty())

        assertDeepEquals(
            BytecodeIr(
                listOf(
                    InstructionSegment(
                        labels = mutableListOf(0),
                        instructions = mutableListOf(
                            Instruction(
                                opcode = OP_SET_EPISODE,
                                args = listOf(IntArg(0)),
                                srcLoc = InstructionSrcLoc(
                                    mnemonic = SrcLoc(2, 5, 11),
                                    args = listOf(ArgSrcLoc(SrcLoc(2, 17, 1), SrcLoc(2, 16, 3))),
                                    trailingArgSeparator = false,
                                ),
                                valid = true,
                            ),
                            Instruction(
                                opcode = OP_BB_MAP_DESIGNATE,
                                args = listOf(IntArg(1), IntArg(2), IntArg(3), IntArg(4)),
                                srcLoc = InstructionSrcLoc(
                                    mnemonic = SrcLoc(3, 5, 16),
                                    args = listOf(
                                        ArgSrcLoc(SrcLoc(3, 22, 1), SrcLoc(3, 21, 3)),
                                        ArgSrcLoc(SrcLoc(3, 25, 1), SrcLoc(3, 24, 3)),
                                        ArgSrcLoc(SrcLoc(3, 28, 1), SrcLoc(3, 27, 3)),
                                        ArgSrcLoc(SrcLoc(3, 31, 1), SrcLoc(3, 30, 3)),
                                    ),
                                    trailingArgSeparator = false,
                                ),
                                valid = true,
                            ),
                            Instruction(
                                opcode = OP_ARG_PUSHL,
                                args = listOf(IntArg(0)),
                                srcLoc = InstructionSrcLoc(
                                    mnemonic = null,
                                    args = listOf(ArgSrcLoc(SrcLoc(4, 23, 1), SrcLoc(4, 22, 3))),
                                    trailingArgSeparator = false,
                                ),
                                valid = true,
                            ),
                            Instruction(
                                opcode = OP_ARG_PUSHW,
                                args = listOf(IntArg(150)),
                                srcLoc = InstructionSrcLoc(
                                    mnemonic = null,
                                    args = listOf(ArgSrcLoc(SrcLoc(4, 26, 3), SrcLoc(4, 25, 5))),
                                    trailingArgSeparator = false,
                                ),
                                valid = true,
                            ),
                            Instruction(
                                opcode = OP_SET_FLOOR_HANDLER,
                                args = emptyList(),
                                srcLoc = InstructionSrcLoc(
                                    mnemonic = SrcLoc(4, 5, 17),
                                    args = listOf(
                                        ArgSrcLoc(SrcLoc(4, 23, 1), SrcLoc(4, 22, 3)),
                                        ArgSrcLoc(SrcLoc(4, 26, 3), SrcLoc(4, 25, 5)),
                                    ),
                                    trailingArgSeparator = false,
                                ),
                                valid = true,
                            ),
                            Instruction(
                                opcode = OP_RET,
                                args = emptyList(),
                                srcLoc = InstructionSrcLoc(
                                    mnemonic = SrcLoc(5, 5, 3),
                                    args = emptyList(),
                                    trailingArgSeparator = false,
                                ),
                                valid = true,
                            ),
                        ),
                        srcLoc = SegmentSrcLoc(labels = mutableListOf(SrcLoc(1, 1, 2))),
                    ),
                    InstructionSegment(
                        labels = mutableListOf(150),
                        instructions = mutableListOf(
                            Instruction(
                                opcode = OP_ARG_PUSHL,
                                args = listOf(IntArg(1)),
                                srcLoc = InstructionSrcLoc(
                                    mnemonic = null,
                                    args = listOf(ArgSrcLoc(SrcLoc(7, 18, 1), SrcLoc(7, 17, 3))),
                                    trailingArgSeparator = false,
                                ),
                                valid = true,
                            ),
                            Instruction(
                                opcode = OP_SET_MAINWARP,
                                args = emptyList(),
                                srcLoc = InstructionSrcLoc(
                                    mnemonic = SrcLoc(7, 5, 12),
                                    args = listOf(
                                        ArgSrcLoc(SrcLoc(7, 18, 1), SrcLoc(7, 17, 3)),
                                    ),
                                    trailingArgSeparator = false,
                                ),
                                valid = true,
                            ),
                            Instruction(
                                opcode = OP_RET,
                                args = emptyList(),
                                srcLoc = InstructionSrcLoc(
                                    mnemonic = SrcLoc(8, 5, 3),
                                    args = emptyList(),
                                    trailingArgSeparator = false,
                                ),
                                valid = true,
                            ),
                        ),
                        srcLoc = SegmentSrcLoc(labels = mutableListOf(SrcLoc(6, 1, 4))),
                    )
                )
            ),
            result.value
        )
    }

    @Test
    fun pass_register_value_via_stack_with_inline_args() {
        val result = assemble(
            """
            0:
                leti r255, 7
                exit r255
                ret
            """.trimIndent().split('\n')
        )

        assertTrue(result is Success)
        assertTrue(result.problems.isEmpty())

        assertDeepEquals(
            BytecodeIr(
                listOf(
                    InstructionSegment(
                        labels = mutableListOf(0),
                        instructions = mutableListOf(
                            Instruction(
                                opcode = OP_LETI,
                                args = listOf(IntArg(255), IntArg(7)),
                                srcLoc = InstructionSrcLoc(
                                    mnemonic = SrcLoc(2, 5, 4),
                                    args = listOf(
                                        ArgSrcLoc(SrcLoc(2, 10, 4), SrcLoc(2, 9, 6)),
                                        ArgSrcLoc(SrcLoc(2, 16, 1), SrcLoc(2, 15, 3)),
                                    ),
                                    trailingArgSeparator = false,
                                ),
                                valid = true,
                            ),
                            Instruction(
                                opcode = OP_ARG_PUSHR,
                                args = listOf(IntArg(255)),
                                srcLoc = InstructionSrcLoc(
                                    mnemonic = null,
                                    args = listOf(ArgSrcLoc(SrcLoc(3, 10, 4), SrcLoc(3, 9, 6))),
                                    trailingArgSeparator = false,
                                ),
                                valid = true,
                            ),
                            Instruction(
                                opcode = OP_EXIT,
                                args = emptyList(),
                                srcLoc = InstructionSrcLoc(
                                    mnemonic = SrcLoc(3, 5, 4),
                                    args = listOf(
                                        ArgSrcLoc(SrcLoc(3, 10, 4), SrcLoc(3, 9, 6)),
                                    ),
                                    trailingArgSeparator = false,
                                ),
                                valid = true,
                            ),
                            Instruction(
                                opcode = OP_RET,
                                args = emptyList(),
                                srcLoc = InstructionSrcLoc(
                                    mnemonic = SrcLoc(4, 5, 3),
                                    args = emptyList(),
                                    trailingArgSeparator = false,
                                ),
                                valid = true,
                            ),
                        ),
                        srcLoc = SegmentSrcLoc(
                            labels = mutableListOf(SrcLoc(1, 1, 2))
                        ),
                    )
                )
            ),
            result.value
        )
    }

    @Test
    fun pass_register_reference_via_stack_with_inline_args() {
        val result = assemble(
            """
            0:
                p_dead_v3 r200, 3
                ret
            """.trimIndent().split('\n')
        )

        assertTrue(result is Success)
        assertTrue(result.problems.isEmpty())

        assertDeepEquals(
            BytecodeIr(
                listOf(
                    InstructionSegment(
                        labels = mutableListOf(0),
                        instructions = mutableListOf(
                            Instruction(
                                opcode = OP_ARG_PUSHB,
                                args = listOf(IntArg(200)),
                                srcLoc = InstructionSrcLoc(
                                    mnemonic = null,
                                    args = listOf(ArgSrcLoc(SrcLoc(2, 15, 4), SrcLoc(2, 14, 6))),
                                    trailingArgSeparator = false,
                                ),
                                valid = true,
                            ),
                            Instruction(
                                opcode = OP_ARG_PUSHL,
                                args = listOf(IntArg(3)),
                                srcLoc = InstructionSrcLoc(
                                    mnemonic = null,
                                    args = listOf(ArgSrcLoc(SrcLoc(2, 21, 1), SrcLoc(2, 20, 3))),
                                    trailingArgSeparator = false,
                                ),
                                valid = true,
                            ),
                            Instruction(
                                opcode = OP_P_DEAD_V3,
                                args = emptyList(),
                                srcLoc = InstructionSrcLoc(
                                    mnemonic = SrcLoc(2, 5, 9),
                                    args = listOf(
                                        ArgSrcLoc(SrcLoc(2, 15, 4), SrcLoc(2, 14, 6)),
                                        ArgSrcLoc(SrcLoc(2, 21, 1), SrcLoc(2, 20, 3)),
                                    ),
                                    trailingArgSeparator = false,
                                ),
                                valid = true,
                            ),
                            Instruction(
                                opcode = OP_RET,
                                args = emptyList(),
                                srcLoc = InstructionSrcLoc(
                                    mnemonic = SrcLoc(3, 5, 3),
                                    args = emptyList(),
                                    trailingArgSeparator = false,
                                ),
                                valid = true,
                            ),
                        ),
                        srcLoc = SegmentSrcLoc(
                            labels = mutableListOf(SrcLoc(1, 1, 2))
                        ),
                    )
                )
            ),
            result.value
        )
    }

    @Test
    fun too_many_arguments() {
        val result = assemble(
            """
            0:
                ret 100
            """.trimIndent().split('\n')
        )

        assertTrue(result is Success)
        // Bytecode contains one invalid instruction.
        assertDeepEquals(
            BytecodeIr(
                listOf(
                    InstructionSegment(
                        labels = mutableListOf(0),
                        instructions = mutableListOf(
                            Instruction(
                                opcode = OP_RET,
                                args = listOf(IntArg(100)),
                                srcLoc = InstructionSrcLoc(
                                    mnemonic = SrcLoc(2, 5, 3),
                                    args = listOf(ArgSrcLoc(SrcLoc(2, 9, 3), SrcLoc(2, 8, 5))),
                                    trailingArgSeparator = false,
                                ),
                                valid = false,
                            ),
                        ),
                        srcLoc = SegmentSrcLoc(
                            labels = mutableListOf(SrcLoc(1, 1, 2)),
                        ),
                    ),
                ),
            ),
            result.value,
        )

        assertEquals(1, result.problems.size)
        val problem = result.problems.first()
        assertTrue(problem is AssemblyProblem)
        assertEquals(2, problem.lineNo)
        assertEquals(5, problem.col)
        assertEquals(7, problem.len)
        assertEquals("Expected 0 arguments, got 1. At 2:5.", problem.message)
    }

    @Test
    fun too_few_arguments() {
        val result = assemble(
            """
            5000:
                leti r100
            """.trimIndent().split('\n')
        )

        assertTrue(result is Success)
        // Bytecode contains one invalid instruction.
        assertDeepEquals(
            BytecodeIr(
                listOf(
                    InstructionSegment(
                        labels = mutableListOf(5000),
                        instructions = mutableListOf(
                            Instruction(
                                opcode = OP_LETI,
                                args = listOf(IntArg(100)),
                                srcLoc = InstructionSrcLoc(
                                    mnemonic = SrcLoc(2, 5, 4),
                                    args = listOf(ArgSrcLoc(SrcLoc(2, 10, 4), SrcLoc(2, 9, 6))),
                                    trailingArgSeparator = false,
                                ),
                                valid = false,
                            ),
                        ),
                        srcLoc = SegmentSrcLoc(
                            labels = mutableListOf(SrcLoc(1, 1, 5)),
                        ),
                    ),
                ),
            ),
            result.value,
        )

        assertEquals(1, result.problems.size)
        val problem = result.problems.first()
        assertTrue(problem is AssemblyProblem)
        assertEquals(2, problem.lineNo)
        assertEquals(5, problem.col)
        assertEquals(9, problem.len)
        assertEquals("Expected 2 arguments, got 1. At 2:5.", problem.message)
    }

    @Test
    fun too_few_arguments_varargs() {
        val result = assemble(
            """
            5000:
                switch_jmp r100
            """.trimIndent().split('\n')
        )

        assertTrue(result is Success)

        // Bytecode contains one invalid instruction.
        assertDeepEquals(
            BytecodeIr(
                listOf(
                    InstructionSegment(
                        labels = mutableListOf(5000),
                        instructions = mutableListOf(
                            Instruction(
                                opcode = OP_SWITCH_JMP,
                                args = listOf(IntArg(100)),
                                srcLoc = InstructionSrcLoc(
                                    mnemonic = SrcLoc(2, 5, 10),
                                    args = listOf(ArgSrcLoc(SrcLoc(2, 16, 4), SrcLoc(2, 15, 6))),
                                    trailingArgSeparator = false,
                                ),
                                valid = false,
                            ),
                        ),
                        srcLoc = SegmentSrcLoc(
                            labels = mutableListOf(SrcLoc(1, 1, 5)),
                        ),
                    ),
                ),
            ),
            result.value,
        )

        assertEquals(1, result.problems.size)
        val problem = result.problems.first()
        assertTrue(problem is AssemblyProblem)
        assertEquals(2, problem.lineNo)
        assertEquals(5, problem.col)
        assertEquals(15, problem.len)
        assertEquals("Expected at least 2 arguments, got 1. At 2:5.", problem.message)
    }
}
