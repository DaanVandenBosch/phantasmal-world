package world.phantasmal.lib.asm

import world.phantasmal.lib.test.LibTestSuite
import kotlin.test.Test
import kotlin.test.assertEquals

class DisassemblyTests : LibTestSuite {
    @Test
    fun vararg_instructions() {
        val ir = BytecodeIr(listOf(
            InstructionSegment(
                labels = mutableListOf(0),
                instructions = mutableListOf(
                    Instruction(
                        opcode = OP_SWITCH_JMP,
                        args = listOf(
                            Arg(90),
                            Arg(100),
                            Arg(101),
                            Arg(102),
                        ),
                    ),
                    Instruction(
                        opcode = OP_RET,
                        args = emptyList()
                    ),
                ),
            )
        ))

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
        val ir = BytecodeIr(listOf(
            InstructionSegment(
                labels = mutableListOf(0),
                instructions = mutableListOf(
                    Instruction(
                        opcode = OP_VA_START,
                    ),
                    Instruction(
                        opcode = OP_ARG_PUSHW,
                        args = listOf(Arg(1337)),
                    ),
                    Instruction(
                        opcode = OP_VA_CALL,
                        args = listOf(Arg(100)),
                    ),
                    Instruction(
                        opcode = OP_VA_END,
                    ),
                    Instruction(
                        opcode = OP_RET,
                    ),
                ),
            )
        ))

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
