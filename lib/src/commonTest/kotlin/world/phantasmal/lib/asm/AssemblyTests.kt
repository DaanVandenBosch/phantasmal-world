package world.phantasmal.lib.asm

import world.phantasmal.core.Success
import world.phantasmal.lib.test.LibTestSuite
import world.phantasmal.lib.test.assertDeepEquals
import kotlin.test.Test
import kotlin.test.assertTrue

class AssemblyTests : LibTestSuite {
    @Test
    fun basic_script() {
        val result = assemble("""
            0:
                set_episode 0
                bb_map_designate 1, 2, 3, 4
                set_floor_handler 0, 150
                ret
            150:
                set_mainwarp 1
                ret
        """.trimIndent().split('\n'))

        assertTrue(result is Success)
        assertTrue(result.problems.isEmpty())

        assertDeepEquals(BytecodeIr(listOf(
            InstructionSegment(
                labels = mutableListOf(0),
                instructions = mutableListOf(
                    Instruction(
                        opcode = OP_SET_EPISODE,
                        args = listOf(Arg(0)),
                        srcLoc = InstructionSrcLoc(
                            mnemonic = SrcLoc(2, 5, 11),
                            args = listOf(SrcLoc(2, 17, 1)),
                            stackArgs = emptyList(),
                        ),
                    ),
                    Instruction(
                        opcode = OP_BB_MAP_DESIGNATE,
                        args = listOf(Arg(1), Arg(2), Arg(3), Arg(4)),
                        srcLoc = InstructionSrcLoc(
                            mnemonic = SrcLoc(3, 5, 16),
                            args = listOf(
                                SrcLoc(3, 22, 1),
                                SrcLoc(3, 25, 1),
                                SrcLoc(3, 28, 1),
                                SrcLoc(3, 31, 1),
                            ),
                            stackArgs = emptyList(),
                        ),
                    ),
                    Instruction(
                        opcode = OP_ARG_PUSHL,
                        args = listOf(Arg(0)),
                        srcLoc = InstructionSrcLoc(
                            mnemonic = null,
                            args = listOf(SrcLoc(4, 23, 1)),
                            stackArgs = emptyList(),
                        ),
                    ),
                    Instruction(
                        opcode = OP_ARG_PUSHW,
                        args = listOf(Arg(150)),
                        srcLoc = InstructionSrcLoc(
                            mnemonic = null,
                            args = listOf(SrcLoc(4, 26, 3)),
                            stackArgs = emptyList(),
                        ),
                    ),
                    Instruction(
                        opcode = OP_SET_FLOOR_HANDLER,
                        args = emptyList(),
                        srcLoc = InstructionSrcLoc(
                            mnemonic = SrcLoc(4, 5, 17),
                            args = emptyList(),
                            stackArgs = listOf(
                                SrcLoc(4, 23, 1),
                                SrcLoc(4, 26, 3),
                            ),
                        ),
                    ),
                    Instruction(
                        opcode = OP_RET,
                        args = emptyList(),
                        srcLoc = InstructionSrcLoc(
                            mnemonic = SrcLoc(5, 5, 3),
                            args = emptyList(),
                            stackArgs = emptyList(),
                        ),
                    ),
                ),
                srcLoc = SegmentSrcLoc(labels = mutableListOf(SrcLoc(1, 1, 2))),
            ),
            InstructionSegment(
                labels = mutableListOf(150),
                instructions = mutableListOf(
                    Instruction(
                        opcode = OP_ARG_PUSHL,
                        args = listOf(Arg(1)),
                        srcLoc = InstructionSrcLoc(
                            mnemonic = null,
                            args = listOf(SrcLoc(7, 18, 1)),
                            stackArgs = emptyList(),
                        ),
                    ),
                    Instruction(
                        opcode = OP_SET_MAINWARP,
                        args = emptyList(),
                        srcLoc = InstructionSrcLoc(
                            mnemonic = SrcLoc(7, 5, 12),
                            args = emptyList(),
                            stackArgs = listOf(SrcLoc(7, 18, 1)),
                        ),
                    ),
                    Instruction(
                        opcode = OP_RET,
                        args = emptyList(),
                        srcLoc = InstructionSrcLoc(
                            mnemonic = SrcLoc(8, 5, 3),
                            args = emptyList(),
                            stackArgs = emptyList(),
                        ),
                    ),
                ),
                srcLoc = SegmentSrcLoc(labels = mutableListOf(SrcLoc(6, 1, 4))),
            )
        )), result.value)
    }

    @Test
    fun pass_register_value_via_stack_with_inline_args() {
        val result = assemble("""
            0:
                leti r255, 7
                exit r255
                ret
        """.trimIndent().split('\n'))

        assertTrue(result is Success)
        assertTrue(result.problems.isEmpty())

        assertDeepEquals(BytecodeIr(
            listOf(
                InstructionSegment(
                    labels = mutableListOf(0),
                    instructions = mutableListOf(
                        Instruction(
                            opcode = OP_LETI,
                            args = listOf(Arg(255), Arg(7)),
                            srcLoc = InstructionSrcLoc(
                                mnemonic = SrcLoc(2, 5, 4),
                                args = listOf(SrcLoc(2, 10, 4), SrcLoc(2, 16, 1)),
                                stackArgs = emptyList(),
                            ),
                        ),
                        Instruction(
                            opcode = OP_ARG_PUSHR,
                            args = listOf(Arg(255)),
                            srcLoc = InstructionSrcLoc(
                                mnemonic = null,
                                args = listOf(SrcLoc(3, 10, 4)),
                                stackArgs = emptyList(),
                            ),
                        ),
                        Instruction(
                            opcode = OP_EXIT,
                            args = emptyList(),
                            srcLoc = InstructionSrcLoc(
                                mnemonic = SrcLoc(3, 5, 4),
                                args = emptyList(),
                                stackArgs = listOf(SrcLoc(3, 10, 4)),
                            ),
                        ),
                        Instruction(
                            opcode = OP_RET,
                            args = emptyList(),
                            srcLoc = InstructionSrcLoc(
                                mnemonic = SrcLoc(4, 5, 3),
                                args = emptyList(),
                                stackArgs = emptyList(),
                            ),
                        ),
                    ),
                    srcLoc = SegmentSrcLoc(
                        labels = mutableListOf(SrcLoc(1, 1, 2))
                    ),
                )
            )
        ), result.value)
    }

    @Test
    fun pass_register_reference_via_stack_with_inline_args() {
        val result = assemble("""
            0:
                p_dead_v3 r200, 3
                ret
        """.trimIndent().split('\n'))

        assertTrue(result is Success)
        assertTrue(result.problems.isEmpty())

        assertDeepEquals(BytecodeIr(
            listOf(
                InstructionSegment(
                    labels = mutableListOf(0),
                    instructions = mutableListOf(
                        Instruction(
                            opcode = OP_ARG_PUSHB,
                            args = listOf(Arg(200)),
                            srcLoc = InstructionSrcLoc(
                                mnemonic = null,
                                args = listOf(SrcLoc(2, 15, 4)),
                                stackArgs = emptyList(),
                            ),
                        ),
                        Instruction(
                            opcode = OP_ARG_PUSHL,
                            args = listOf(Arg(3)),
                            srcLoc = InstructionSrcLoc(
                                mnemonic = null,
                                args = listOf(SrcLoc(2, 21, 1)),
                                stackArgs = emptyList(),
                            ),
                        ),
                        Instruction(
                            opcode = OP_P_DEAD_V3,
                            args = emptyList(),
                            srcLoc = InstructionSrcLoc(
                                mnemonic = SrcLoc(2, 5, 9),
                                args = emptyList(),
                                stackArgs = listOf(SrcLoc(2, 15, 4), SrcLoc(2, 21, 1)),
                            ),
                        ),
                        Instruction(
                            opcode = OP_RET,
                            args = emptyList(),
                            srcLoc = InstructionSrcLoc(
                                mnemonic = SrcLoc(3, 5, 3),
                                args = emptyList(),
                                stackArgs = emptyList(),
                            ),
                        ),
                    ),
                    srcLoc = SegmentSrcLoc(
                        labels = mutableListOf(SrcLoc(1, 1, 2))
                    ),
                )
            )
        ), result.value)
    }
}
