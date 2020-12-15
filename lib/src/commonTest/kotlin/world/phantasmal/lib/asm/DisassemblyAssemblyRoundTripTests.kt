package world.phantasmal.lib.asm

import world.phantasmal.core.Success
import world.phantasmal.lib.fileFormats.quest.parseBin
import world.phantasmal.lib.fileFormats.quest.parseBytecode
import world.phantasmal.lib.test.LibTestSuite
import world.phantasmal.lib.test.assertDeepEquals
import world.phantasmal.lib.test.readFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DisassemblyAssemblyRoundTripTests : LibTestSuite() {
    @Test
    fun assembling_disassembled_bytecode_should_result_in_the_same_IR() = asyncTest {
        assembling_disassembled_bytecode_should_result_in_the_same_IR(inlineStackArgs = false)
    }

    @Test
    fun assembling_disassembled_bytecode_should_result_in_the_same_IR_inline_args() = asyncTest {
        assembling_disassembled_bytecode_should_result_in_the_same_IR(inlineStackArgs = true)
    }

    private suspend fun assembling_disassembled_bytecode_should_result_in_the_same_IR(
        inlineStackArgs: Boolean,
    ) {
        val bin = parseBin(readFile("/quest27_e_decompressed.bin"))
        val expectedIr = parseBytecode(
            bin.bytecode,
            bin.labelOffsets,
            setOf(0),
            dcGcFormat = false,
            lenient = false,
        ).unwrap()

        val assemblyResult =
            assemble(disassemble(expectedIr, inlineStackArgs), inlineStackArgs)

        assertTrue(assemblyResult.problems.isEmpty())
        assertTrue(assemblyResult is Success)
        assertDeepEquals(expectedIr, assemblyResult.value, ignoreSrcLocs = true)
    }

    @Test
    fun disassembling_assembled_bytecode_should_result_in_the_same_ASM() = asyncTest {
        disassembling_assembled_bytecode_should_result_in_the_same_ASM(inlineStackArgs = false)
    }

    @Test
    fun disassembling_assembled_bytecode_should_result_in_the_same_ASM_inline_args() = asyncTest {
        disassembling_assembled_bytecode_should_result_in_the_same_ASM(inlineStackArgs = true)
    }

    private suspend fun disassembling_assembled_bytecode_should_result_in_the_same_ASM(
        inlineStackArgs: Boolean,
    ) {
        val bin = parseBin(readFile("/quest27_e_decompressed.bin"))
        val ir = parseBytecode(
            bin.bytecode,
            bin.labelOffsets,
            setOf(0),
            dcGcFormat = false,
            lenient = false,
        ).unwrap()

        val expectedAsm = disassemble(ir, inlineStackArgs)
        val actualAsm =
            disassemble(assemble(expectedAsm, inlineStackArgs).unwrap(), inlineStackArgs)

        assertDeepEquals(expectedAsm, actualAsm, ::assertEquals)
    }
}
