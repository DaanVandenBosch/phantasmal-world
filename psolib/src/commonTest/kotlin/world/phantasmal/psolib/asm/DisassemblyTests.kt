package world.phantasmal.psolib.asm

import world.phantasmal.psolib.test.LibTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals

class DisassemblyTests : LibTestSuite {
    @Test
    fun vararg_instructions() {
        val ir = BytecodeIr(
            listOf(
                InstructionSegment(
                    labels = mutableListOf(0),
                    instructions = mutableListOf(
                        Instruction(
                            opcode = OP_SWITCH_JMP,
                            args = listOf(
                                IntArg(90),
                                IntArg(100),
                                IntArg(101),
                                IntArg(102),
                            ),
                            srcLoc = null,
                            valid = true,
                        ),
                        Instruction(
                            opcode = OP_RET,
                            args = emptyList(),
                            srcLoc = null,
                            valid = true,
                        ),
                    ),
                )
            )
        )

        val asm = """
            |.code
            |
            |0:
            |    switch_jmp r90, 100, 101, 102
            |    ret
            |
        """.trimMargin()

        testWithAllOptions(ir, asm, asm)
    }

    // arg_push* instructions should always be output when in a va list whether inline stack
    // arguments is on or off.
    @Test
    fun va_list_instructions() {
        val ir = BytecodeIr(
            listOf(
                InstructionSegment(
                    labels = mutableListOf(0),
                    instructions = mutableListOf(
                        Instruction(
                            opcode = OP_VA_START,
                            args = emptyList(),
                            srcLoc = null,
                            valid = true,
                        ),
                        Instruction(
                            opcode = OP_ARG_PUSHW,
                            args = listOf(IntArg(1337)),
                            srcLoc = null,
                            valid = true,
                        ),
                        Instruction(
                            opcode = OP_VA_CALL,
                            args = listOf(IntArg(100)),
                            srcLoc = null,
                            valid = true,
                        ),
                        Instruction(
                            opcode = OP_VA_END,
                            args = emptyList(),
                            srcLoc = null,
                            valid = true,
                        ),
                        Instruction(
                            opcode = OP_RET,
                            args = emptyList(),
                            srcLoc = null,
                            valid = true,
                        ),
                    ),
                )
            )
        )

        val asm = """
            |.code
            |
            |0:
            |    va_start
            |    arg_pushw 1337
            |    va_call 100
            |    va_end
            |    ret
            |
        """.trimMargin()

        testWithAllOptions(ir, asm, asm)
    }

    private fun testWithAllOptions(
        ir: BytecodeIr,
        expectedInlineAsm: String,
        expectedManualAsm: String,
    ) {
        val asmInline = disassemble(ir, inlineStackArgs = true)

        assertEquals(
            expectedInlineAsm.split('\n'),
            asmInline,
            "With inlineStackArgs",
        )

        val asmManual = disassemble(ir, inlineStackArgs = false)

        assertEquals(
            expectedManualAsm.split('\n'),
            asmManual,
            "Without inlineStackArgs",
        )
    }
}
